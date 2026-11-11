package com.isaacggr.finance_app.config;

import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.support.AbstractWebIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SecurityConfigTest extends AbstractWebIntegrationTest {

    @Test
    @DisplayName("Deve retornar 401 Unauthorized com WWW-Authenticate Bearer quando token estiver ausente ou inválido")
    void shouldReturn401WithBearerHeader_WhenTokenMissingOrInvalid() throws Exception {
        assertUnauthorizedBearer(null);
        assertUnauthorizedBearer("Bearer token.invalido.aqui");
    }

    private void assertUnauthorizedBearer(String authorizationHeader) throws Exception {
        var request = get("/transactions");
        if (authorizationHeader != null) {
            request.header(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }

        mockMvc.perform(request)
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Deve permitir acesso (200 OK) com token JWT válido de usuário ativo e verificado")
    @SuppressWarnings("unchecked")
    void shouldAllowAccess_WhenTokenValid() throws Exception {
        User user = createVerifiedUser("Isaac", "isaac@example.com", "Senha123!");
        String jwt = tokenService.generateToken(user);

        when(userRepository.findByIdAndDeletedFalse(user.getId())).thenReturn(Optional.of(user));
        when(transactionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("Deve manter 403 Forbidden no login quando conta existe mas e-mail não foi verificado")
    void shouldReturn403_WhenLoginWithUnverifiedEmail() throws Exception {
        String rawPassword = "SenhaForte123";
        User unverified = new User("Pendente", "pendente@example.com", passwordEncoder.encode(rawPassword));
        ReflectionTestUtils.setField(unverified, "id", UUID.randomUUID());

        when(userRepository.findByEmail("pendente@example.com")).thenReturn(Optional.of(unverified));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"pendente@example.com\",\"password\":\"SenhaForte123\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Deve aceitar CORS preflight das origens permitidas (5173/5174), remover barras finais e rejeitar origem não configurada")
    void shouldEnforceConfiguredCorsOrigins() throws Exception {
        mockMvc.perform(options("/transactions")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));

        mockMvc.perform(options("/transactions")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5174")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5174"));

        mockMvc.perform(options("/transactions")
                        .header(HttpHeaders.ORIGIN, "https://site-malicioso.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));

        SecurityConfig configWithTrailingSlashes = new SecurityConfig(
                null,
                null,
                "http://localhost:5174/, https://finangame.vercel.app///"
        );
        var corsConfig = configWithTrailingSlashes.corsConfigurationSource()
                .getCorsConfiguration(new MockHttpServletRequest("GET", "/auth/login"));
        assertNotNull(corsConfig);
        assertEquals(
                List.of("http://localhost:5174", "https://finangame.vercel.app"),
                corsConfig.getAllowedOriginPatterns()
        );
    }

    @Test
    @DisplayName("Deve expor /actuator/health publicamente e manter demais endpoints do actuator protegidos")
    void shouldExposeOnlyHealthEndpointPublicly() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));
    }
}
