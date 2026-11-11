package com.isaacggr.finance_app.config;

import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.repository.BudgetRepository;
import com.isaacggr.finance_app.repository.TransactionRepository;
import com.isaacggr.finance_app.repository.UserRepository;
import com.isaacggr.finance_app.repository.VerificationTokenRepository;
import com.isaacggr.finance_app.service.TokenService;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,org.springframework.boot.orm.jpa.autoconfigure.HibernateJpaAutoConfiguration,org.springframework.boot.data.jpa.autoconfigure.JpaRepositoriesAutoConfiguration"
})
class SecurityConfigTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter springSecurityFilterChain;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private TransactionRepository transactionRepository;

    @MockitoBean
    private BudgetRepository budgetRepository;

    @MockitoBean
    private VerificationTokenRepository verificationTokenRepository;

    @MockitoBean
    private JavaMailSender mailSender;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    @Test
    @DisplayName("Deve retornar 401 Unauthorized com WWW-Authenticate Bearer quando token estiver ausente")
    void shouldReturn401WithBearerHeader_WhenTokenMissing() throws Exception {
        mockMvc.perform(get("/transactions"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Deve retornar 401 Unauthorized quando token JWT for inválido ou expirado")
    void shouldReturn401_WhenTokenInvalid() throws Exception {
        mockMvc.perform(get("/transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token.invalido.aqui"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Deve permitir acesso (200 OK) com token JWT válido de usuário ativo e verificado")
    @SuppressWarnings("unchecked")
    void shouldAllowAccess_WhenTokenValid() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = new User("Isaac", "isaac@example.com", passwordEncoder.encode("Senha123!"));
        user.verifyEmail();
        ReflectionTestUtils.setField(user, "id", userId);

        String jwt = tokenService.generateToken(user);

        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.of(user));
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
                .getCorsConfiguration(new org.springframework.mock.web.MockHttpServletRequest("GET", "/auth/login"));
        org.junit.jupiter.api.Assertions.assertNotNull(corsConfig);
        org.junit.jupiter.api.Assertions.assertEquals(
                java.util.List.of("http://localhost:5174", "https://finangame.vercel.app"),
                corsConfig.getAllowedOriginPatterns()
        );
    }
}
