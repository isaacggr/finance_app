package com.isaacggr.finance_app.service;

import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.dto.LoginRequestDTO;
import com.isaacggr.finance_app.dto.LoginResponseDTO;
import com.isaacggr.finance_app.exception.UnauthorizedException;
import com.isaacggr.finance_app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    private AuthService authService;
    private final long expiration = 86400000;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, tokenService, expiration);
        sampleUser = new User("Isaac Gregorio", "isaac@example.com", "encodedPassword123");
        ReflectionTestUtils.setField(sampleUser, "id", UUID.randomUUID());
    }

    @Test
    @DisplayName("Deve realizar login com sucesso e retornar token JWT")
    void login_Success() {
        LoginRequestDTO dto = new LoginRequestDTO("isaac@example.com", "plainPassword123");

        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches(dto.password(), sampleUser.getPasswordHash())).thenReturn(true);
        when(tokenService.generateToken(sampleUser)).thenReturn("fake.jwt.token");

        LoginResponseDTO response = authService.login(dto);

        assertNotNull(response);
        assertEquals("fake.jwt.token", response.token());
        assertEquals("Bearer", response.type());
        assertEquals(expiration, response.expiresIn());
        verify(tokenService).generateToken(sampleUser);
    }

    @Test
    @DisplayName("Deve lançar UnauthorizedException quando o usuário não for encontrado")
    void login_ThrowsUnauthorized_WhenUserNotFound() {
        LoginRequestDTO dto = new LoginRequestDTO("notfound@example.com", "password");

        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.empty());

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authService.login(dto)
        );

        assertEquals("Credenciais inválidas", exception.getMessage());
        verify(tokenService, never()).generateToken(any());
    }

    @Test
    @DisplayName("Deve lançar UnauthorizedException quando o usuário estiver desativado/deletado")
    void login_ThrowsUnauthorized_WhenUserIsDeleted() {
        LoginRequestDTO dto = new LoginRequestDTO("isaac@example.com", "plainPassword123");
        sampleUser.markAsDeleted();

        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.of(sampleUser));

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authService.login(dto)
        );

        assertEquals("Credenciais inválidas", exception.getMessage());
        verify(tokenService, never()).generateToken(any());
    }

    @Test
    @DisplayName("Deve lançar UnauthorizedException quando a senha estiver incorreta")
    void login_ThrowsUnauthorized_WhenPasswordIsIncorrect() {
        LoginRequestDTO dto = new LoginRequestDTO("isaac@example.com", "wrongPassword");

        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches(dto.password(), sampleUser.getPasswordHash())).thenReturn(false);

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authService.login(dto)
        );

        assertEquals("Credenciais inválidas", exception.getMessage());
        verify(tokenService, never()).generateToken(any());
    }
}

