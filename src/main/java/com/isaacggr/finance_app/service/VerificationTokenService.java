package com.isaacggr.finance_app.service;

import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.domain.entity.VerificationToken;
import com.isaacggr.finance_app.domain.enums.TokenType;
import com.isaacggr.finance_app.exception.BusinessException;
import com.isaacggr.finance_app.repository.VerificationTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class VerificationTokenService {

    private static final Logger log = LoggerFactory.getLogger(VerificationTokenService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final VerificationTokenRepository verificationTokenRepository;

    public VerificationTokenService(VerificationTokenRepository verificationTokenRepository) {
        this.verificationTokenRepository = verificationTokenRepository;
    }

    // ==========================================
    // CRIAÇÃO DE CÓDIGO DE ATIVAÇÃO DE E-MAIL (6 Dígitos - 24h)
    // ==========================================
    public VerificationToken createEmailVerificationToken(User user) {
        invalidateExistingTokens(user.getId(), TokenType.EMAIL_VERIFICATION);

        String code = generateNumericCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);

        VerificationToken verificationToken = new VerificationToken(user, code, TokenType.EMAIL_VERIFICATION, expiresAt);
        return verificationTokenRepository.save(verificationToken);
    }

    // ==========================================
    // CRIAÇÃO DE CÓDIGO DE RESET DE SENHA (6 Dígitos - 30min)
    // ==========================================
    public VerificationToken createPasswordResetToken(User user) {
        invalidateExistingTokens(user.getId(), TokenType.PASSWORD_RESET);

        String code = generateNumericCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(30);

        VerificationToken verificationToken = new VerificationToken(user, code, TokenType.PASSWORD_RESET, expiresAt);
        return verificationTokenRepository.save(verificationToken);
    }

    private String generateNumericCode() {
        int code = SECURE_RANDOM.nextInt(1_000_000);
        return String.format("%06d", code);
    }

    // ==========================================
    // VALIDAÇÃO E CONSUMO DO TOKEN (Uso Único)
    // ==========================================
    public VerificationToken validateToken(String token, TokenType type) {
        VerificationToken verificationToken = verificationTokenRepository
                .findByTokenAndTypeAndUsedFalse(token, type)
                .orElseThrow(() -> new BusinessException("Token inválido ou já utilizado"));

        if (verificationToken.isExpired()) {
            verificationToken.markAsUsed();
            verificationTokenRepository.save(verificationToken);
            throw new BusinessException("Token expirado. Por favor, solicite um novo link.");
        }

        verificationToken.markAsUsed();
        return verificationTokenRepository.save(verificationToken);
    }

    // ==========================================
    // LIMPEZA AGENDADA DE TOKENS EXPIRADOS
    // Executa diariamente às 03:00 da manhã
    // ==========================================
    @Scheduled(cron = "0 0 3 * * ?")
    public void purgeExpiredTokens() {
        int deleted = verificationTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        if (deleted > 0) {
            log.info("🧹 Expurgo de tokens agendado: {} registros expirados removidos.", deleted);
        }
    }

    private void invalidateExistingTokens(UUID userId, TokenType type) {
        List<VerificationToken> pendingTokens = verificationTokenRepository.findByUserIdAndTypeAndUsedFalse(userId, type);
        if (!pendingTokens.isEmpty()) {
            pendingTokens.forEach(VerificationToken::markAsUsed);
            verificationTokenRepository.saveAll(pendingTokens);
        }
    }
}

