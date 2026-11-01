package com.isaacggr.finance_app.service;

import com.isaacggr.finance_app.domain.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TokenServiceTest {

    private TokenService tokenService;
    private final String secret = "test-secret-key-with-sufficient-length-for-hmac256";
    private final long expiration = 3600000; // 1 hora
    private User sampleUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(secret, expiration);
        sampleUser = new User("Isaac Gregorio", "isaac@example.com", "hash123");
        userId = UUID.randomUUID();
        ReflectionTestUtils.setField(sampleUser, "id", userId);
    }

    @Test
    @DisplayName("Deve gerar um token JWT válido para um usuário")
    void generateToken_Success() {
        String token = tokenService.generateToken(sampleUser);

        assertNotNull(token);
        assertFalse(token.isBlank());
        // JWT é composto por 3 partes separadas por ponto: header.payload.signature
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    @DisplayName("Deve validar o token e retornar o subject (ID do usuário)")
    void validateToken_ValidToken_ReturnsUserId() {
        String token = tokenService.generateToken(sampleUser);

        String subject = tokenService.validateToken(token);

        assertNotNull(subject);
        assertEquals(userId.toString(), subject);
    }

    @Test
    @DisplayName("Deve retornar null quando o token for inválido ou adulterado")
    void validateToken_InvalidToken_ReturnsNull() {
        String invalidToken = "invalid.token.structure";

        String subject = tokenService.validateToken(invalidToken);

        assertNull(subject);
    }

    @Test
    @DisplayName("Deve retornar null quando o token estiver expirado")
    void validateToken_ExpiredToken_ReturnsNull() {
        // Token com expiração no passado (-1 segundo)
        TokenService expiredTokenService = new TokenService(secret, -1000);
        String expiredToken = expiredTokenService.generateToken(sampleUser);

        String subject = expiredTokenService.validateToken(expiredToken);

        assertNull(subject);
    }
}

