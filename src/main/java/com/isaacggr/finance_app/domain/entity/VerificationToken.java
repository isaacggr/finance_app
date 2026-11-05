package com.isaacggr.finance_app.domain.entity;

import com.isaacggr.finance_app.domain.enums.TokenType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "verification_tokens", indexes = {
        @Index(name = "idx_verification_tokens_user_type", columnList = "user_id, type"),
        @Index(name = "idx_verification_tokens_expires", columnList = "expires_at"),
        @Index(name = "idx_verification_tokens_token_type", columnList = "token, type")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VerificationToken {

    public static final int MAX_ATTEMPTS = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TokenType type;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public VerificationToken(User user, String token, TokenType type, LocalDateTime expiresAt) {
        this.user = user;
        this.token = token;
        this.type = type;
        this.expiresAt = expiresAt;
        this.used = false;
        this.attempts = 0;
        this.createdAt = LocalDateTime.now();
    }

    // ======================
    // REGRAS DE DOMÍNIO
    // ======================

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    public boolean isValid() {
        return !this.used && !isExpired() && !isMaxAttemptsReached();
    }

    public void markAsUsed() {
        this.used = true;
    }

    public void recordFailedAttempt() {
        this.attempts++;
        if (this.attempts >= MAX_ATTEMPTS) {
            this.used = true;
        }
    }

    public boolean isMaxAttemptsReached() {
        return this.attempts >= MAX_ATTEMPTS;
    }
}
