package com.isaacggr.finance_app.service;

import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.domain.entity.VerificationToken;
import com.isaacggr.finance_app.domain.enums.TokenType;
import com.isaacggr.finance_app.exception.BusinessException;
import com.isaacggr.finance_app.repository.VerificationTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationTokenServiceTest {

    @Mock
    private VerificationTokenRepository verificationTokenRepository;

    @InjectMocks
    private VerificationTokenService verificationTokenService;

    private User sampleUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sampleUser = new User("Isaac Gregorio", "isaac@example.com", "hash123");
        ReflectionTestUtils.setField(sampleUser, "id", userId);
    }

    @Test
    @DisplayName("Deve gerar token de ativação de e-mail e invalidar pendentes anteriores")
    void createEmailVerificationToken_Success() {
        VerificationToken oldToken = new VerificationToken(sampleUser, "old-token", TokenType.EMAIL_VERIFICATION, LocalDateTime.now().plusHours(12));
        assertFalse(oldToken.isUsed());

        when(verificationTokenRepository.findByUserIdAndTypeAndUsedFalse(userId, TokenType.EMAIL_VERIFICATION))
                .thenReturn(List.of(oldToken));
        when(verificationTokenRepository.save(any(VerificationToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VerificationToken newToken = verificationTokenService.createEmailVerificationToken(sampleUser);

        assertNotNull(newToken);
        assertNotNull(newToken.getToken());
        assertEquals(TokenType.EMAIL_VERIFICATION, newToken.getType());
        assertFalse(newToken.isUsed());
        assertTrue(oldToken.isUsed());
        verify(verificationTokenRepository).saveAll(List.of(oldToken));
        verify(verificationTokenRepository).save(any(VerificationToken.class));
    }

    @Test
    @DisplayName("Deve validar token ativo com sucesso e marcá-lo como utilizado")
    void validateToken_Success() {
        VerificationToken token = new VerificationToken(sampleUser, "valid-token", TokenType.EMAIL_VERIFICATION, LocalDateTime.now().plusHours(1));

        when(verificationTokenRepository.findByTokenAndTypeAndUsedFalse("valid-token", TokenType.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(token));
        when(verificationTokenRepository.save(any(VerificationToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VerificationToken result = verificationTokenService.validateToken("valid-token", TokenType.EMAIL_VERIFICATION);

        assertNotNull(result);
        assertTrue(result.isUsed());
        verify(verificationTokenRepository).save(token);
    }

    @Test
    @DisplayName("Deve lançar BusinessException quando o token não for encontrado ou já tiver sido usado")
    void validateToken_ThrowsBusinessException_WhenTokenNotFoundOrUsed() {
        when(verificationTokenRepository.findByTokenAndTypeAndUsedFalse("invalid-token", TokenType.EMAIL_VERIFICATION))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> verificationTokenService.validateToken("invalid-token", TokenType.EMAIL_VERIFICATION)
        );

        assertEquals("Token inválido ou já utilizado", exception.getMessage());
    }

    @Test
    @DisplayName("Deve lançar BusinessException quando o token estiver expirado")
    void validateToken_ThrowsBusinessException_WhenTokenExpired() {
        VerificationToken expiredToken = new VerificationToken(sampleUser, "expired-token", TokenType.EMAIL_VERIFICATION, LocalDateTime.now().minusMinutes(5));

        when(verificationTokenRepository.findByTokenAndTypeAndUsedFalse("expired-token", TokenType.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(expiredToken));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> verificationTokenService.validateToken("expired-token", TokenType.EMAIL_VERIFICATION)
        );

        assertEquals("Token expirado. Por favor, solicite um novo link.", exception.getMessage());
        assertTrue(expiredToken.isUsed());
        verify(verificationTokenRepository).save(expiredToken);
    }

    @Test
    @DisplayName("Deve validar código de 6 dígitos com e-mail com sucesso")
    void validateTokenWithEmail_Success() {
        VerificationToken token = new VerificationToken(sampleUser, "123456", TokenType.EMAIL_VERIFICATION, LocalDateTime.now().plusHours(1));

        when(verificationTokenRepository.findByUserEmailAndTypeAndUsedFalse("isaac@example.com", TokenType.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(token));
        when(verificationTokenRepository.save(any(VerificationToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VerificationToken result = verificationTokenService.validateTokenWithEmail("isaac@example.com", "123456", TokenType.EMAIL_VERIFICATION);

        assertNotNull(result);
        assertTrue(result.isUsed());
        assertEquals(0, result.getAttempts());
        verify(verificationTokenRepository).save(token);
    }

    @Test
    @DisplayName("Deve falhar com código incorreto e decrementar tentativas restantes")
    void validateTokenWithEmail_IncorrectCode_IncrementsAttempts() {
        VerificationToken token = new VerificationToken(sampleUser, "123456", TokenType.EMAIL_VERIFICATION, LocalDateTime.now().plusHours(1));

        when(verificationTokenRepository.findByUserEmailAndTypeAndUsedFalse("isaac@example.com", TokenType.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(token));
        when(verificationTokenRepository.save(any(VerificationToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> verificationTokenService.validateTokenWithEmail("isaac@example.com", "999999", TokenType.EMAIL_VERIFICATION)
        );

        assertTrue(exception.getMessage().contains("Código incorreto. Tentativas restantes: 4"));
        assertEquals(1, token.getAttempts());
        assertFalse(token.isUsed());
        verify(verificationTokenRepository).save(token);
    }

    @Test
    @DisplayName("Deve bloquear e invalidar token ao atingir o limite de 5 tentativas")
    void validateTokenWithEmail_LocksAndInvalidatesToken_WhenMaxAttemptsReached() {
        VerificationToken token = new VerificationToken(sampleUser, "123456", TokenType.EMAIL_VERIFICATION, LocalDateTime.now().plusHours(1));
        ReflectionTestUtils.setField(token, "attempts", 4);

        when(verificationTokenRepository.findByUserEmailAndTypeAndUsedFalse("isaac@example.com", TokenType.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(token));
        when(verificationTokenRepository.save(any(VerificationToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> verificationTokenService.validateTokenWithEmail("isaac@example.com", "000000", TokenType.EMAIL_VERIFICATION)
        );

        assertTrue(exception.getMessage().contains("Número máximo de tentativas excedido (5)"));
        assertEquals(5, token.getAttempts());
        assertTrue(token.isUsed());
        verify(verificationTokenRepository).save(token);
    }

    @Test
    @DisplayName("Deve falhar ao validar token expirado com email")
    void validateTokenWithEmail_ThrowsBusinessException_WhenExpired() {
        VerificationToken expiredToken = new VerificationToken(sampleUser, "123456", TokenType.EMAIL_VERIFICATION, LocalDateTime.now().minusMinutes(5));

        when(verificationTokenRepository.findByUserEmailAndTypeAndUsedFalse("isaac@example.com", TokenType.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(expiredToken));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> verificationTokenService.validateTokenWithEmail("isaac@example.com", "123456", TokenType.EMAIL_VERIFICATION)
        );

        assertEquals("Código expirado. Por favor, solicite um novo código.", exception.getMessage());
        assertTrue(expiredToken.isUsed());
        verify(verificationTokenRepository).save(expiredToken);
    }

    @Test
    @DisplayName("Deve executar expurgo de tokens expirados agendado")
    void purgeExpiredTokens_Success() {
        when(verificationTokenRepository.deleteByExpiresAtBefore(any(LocalDateTime.class))).thenReturn(5);

        assertDoesNotThrow(() -> verificationTokenService.purgeExpiredTokens());

        verify(verificationTokenRepository, times(1)).deleteByExpiresAtBefore(any(LocalDateTime.class));
    }
}

