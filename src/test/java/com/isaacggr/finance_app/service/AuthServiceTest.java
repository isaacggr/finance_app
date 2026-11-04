package com.isaacggr.finance_app.service;

import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.domain.entity.VerificationToken;
import com.isaacggr.finance_app.domain.enums.TokenType;
import com.isaacggr.finance_app.dto.ForgotPasswordRequestDTO;
import com.isaacggr.finance_app.dto.LoginRequestDTO;
import com.isaacggr.finance_app.dto.LoginResponseDTO;
import com.isaacggr.finance_app.dto.ResetPasswordRequestDTO;
import com.isaacggr.finance_app.exception.BusinessException;
import com.isaacggr.finance_app.exception.ForbiddenException;
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

import java.time.LocalDateTime;
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

    @Mock
    private VerificationTokenService verificationTokenService;

    @Mock
    private EmailService emailService;

    private AuthService authService;
    private final long expiration = 86400000;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                tokenService,
                verificationTokenService,
                emailService,
                expiration
        );
        sampleUser = new User("Isaac Gregorio", "isaac@example.com", "encodedPassword123");
        sampleUser.verifyEmail();
        ReflectionTestUtils.setField(sampleUser, "id", UUID.randomUUID());
    }

    @Test
    @DisplayName("Deve realizar login com sucesso e retornar token JWT quando o e-mail estiver verificado")
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
    @DisplayName("Deve lançar ForbiddenException (403) quando o e-mail não tiver sido verificado")
    void login_ThrowsForbidden_WhenEmailNotVerified() {
        User unverifiedUser = new User("Pendente", "pendente@example.com", "encodedPassword123");
        ReflectionTestUtils.setField(unverifiedUser, "id", UUID.randomUUID());
        LoginRequestDTO dto = new LoginRequestDTO("pendente@example.com", "plainPassword123");

        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.of(unverifiedUser));
        when(passwordEncoder.matches(dto.password(), unverifiedUser.getPasswordHash())).thenReturn(true);

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> authService.login(dto)
        );

        assertEquals("Por favor, confirme seu e-mail antes de efetuar login.", exception.getMessage());
        verify(tokenService, never()).generateToken(any());
    }

    @Test
    @DisplayName("Deve confirmar o e-mail com sucesso e ativar a conta")
    void verifyEmail_Success() {
        User pendingUser = new User("Ativacao", "ativacao@example.com", "encodedPassword123");
        assertFalse(pendingUser.isEmailVerified());

        VerificationToken token = new VerificationToken(pendingUser, "valid-token-123", TokenType.EMAIL_VERIFICATION, LocalDateTime.now().plusHours(24));

        when(verificationTokenService.validateToken("valid-token-123", TokenType.EMAIL_VERIFICATION)).thenReturn(token);

        authService.verifyEmail("valid-token-123");

        assertTrue(pendingUser.isEmailVerified());
        verify(userRepository, times(1)).save(pendingUser);
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

    @Test
    @DisplayName("Deve gerar código e enviar e-mail de recuperação quando o e-mail existir")
    void forgotPassword_Success_WhenUserExists() {
        ForgotPasswordRequestDTO dto = new ForgotPasswordRequestDTO("isaac@example.com");
        VerificationToken token = new VerificationToken(sampleUser, "654321", TokenType.PASSWORD_RESET, LocalDateTime.now().plusMinutes(30));

        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.of(sampleUser));
        when(verificationTokenService.createPasswordResetToken(sampleUser)).thenReturn(token);

        authService.forgotPassword(dto);

        verify(verificationTokenService).createPasswordResetToken(sampleUser);
        verify(emailService).sendPasswordResetEmail(eq("isaac@example.com"), eq("Isaac Gregorio"), eq("654321"));
    }

    @Test
    @DisplayName("Deve silenciar e não vazar erro quando o e-mail não existir na recuperação (Anti-Enumeração OWASP)")
    void forgotPassword_SilentSuccess_WhenUserNotFound() {
        ForgotPasswordRequestDTO dto = new ForgotPasswordRequestDTO("desconhecido@example.com");

        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> authService.forgotPassword(dto));

        verify(verificationTokenService, never()).createPasswordResetToken(any());
        verify(emailService, never()).sendPasswordResetEmail(any(), any(), any());
    }

    @Test
    @DisplayName("Deve redefinir senha com sucesso quando código e e-mail forem válidos")
    void resetPassword_Success() {
        ResetPasswordRequestDTO dto = new ResetPasswordRequestDTO("isaac@example.com", "654321", "NovaSenhaForte123!");
        VerificationToken token = new VerificationToken(sampleUser, "654321", TokenType.PASSWORD_RESET, LocalDateTime.now().plusMinutes(30));

        when(verificationTokenService.validateToken("654321", TokenType.PASSWORD_RESET)).thenReturn(token);
        when(passwordEncoder.encode("NovaSenhaForte123!")).thenReturn("newBcryptHash123");

        authService.resetPassword(dto);

        assertEquals("newBcryptHash123", sampleUser.getPasswordHash());
        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("Deve lançar BusinessException quando o código pertencer a outro usuário")
    void resetPassword_ThrowsBusinessException_WhenEmailMismatch() {
        User otherUser = new User("Outro", "outro@example.com", "hash");
        ResetPasswordRequestDTO dto = new ResetPasswordRequestDTO("isaac@example.com", "654321", "NovaSenhaForte123!");
        VerificationToken token = new VerificationToken(otherUser, "654321", TokenType.PASSWORD_RESET, LocalDateTime.now().plusMinutes(30));

        when(verificationTokenService.validateToken("654321", TokenType.PASSWORD_RESET)).thenReturn(token);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.resetPassword(dto)
        );

        assertEquals("O código informado não pertence ao e-mail fornecido.", exception.getMessage());
        verify(userRepository, never()).save(any());
    }
}

