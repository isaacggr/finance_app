package com.isaacggr.finance_app.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "budgets", indexes = {
        @Index(name = "idx_budgets_user_period", columnList = "user_id, month, year")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_budgets_user_cat_period", columnNames = {"user_id", "category", "month", "year"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 30)
    private String category;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private int month;

    @Column(nullable = false)
    private int year;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Budget(User user, String category, BigDecimal amount, int month, int year) {
        this.user = user;
        this.category = Transaction.normalizeCategory(category);
        this.amount = Transaction.normalizeMonetaryAmount(amount);
        this.month = month;
        this.year = year;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Regras ricas de domínio
    public void updateAmount(BigDecimal newAmount) {
        this.amount = Transaction.normalizeMonetaryAmount(newAmount);
    }

    public void update(String category, BigDecimal amount, int month, int year) {
        this.category = Transaction.normalizeCategory(category);
        this.amount = Transaction.normalizeMonetaryAmount(amount);
        this.month = month;
        this.year = year;
    }
}
