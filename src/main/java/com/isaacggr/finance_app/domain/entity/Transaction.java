package com.isaacggr.finance_app.domain.entity;

import com.isaacggr.finance_app.domain.enums.TransactionType;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_transactions_user_date", columnList = "user_id, date")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    @Column(nullable = false, length = 30)
    private String category;

    @Column(nullable = false)
    private LocalDate date;

    @Column(length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    // =========================
    // CONSTRUTOR
    // =========================
    public Transaction(
            User user,
            BigDecimal amount,
            TransactionType type,
            String category,
            LocalDate date,
            String description) {

        this.user = user;
        this.amount = amount;
        this.type = type;
        this.category = normalizeCategory(category);
        this.date = date;
        this.description = description;
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

    // =========================
    // MUDANÇA DE ESTADO
    // =========================
    public void update(
            BigDecimal amount,
            TransactionType type,
            String category,
            LocalDate date,
            String description) {

        this.amount = amount;
        this.type = type;
        this.category = normalizeCategory(category);
        this.date = date;
        this.description = description;
    }

    public static String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return "OUTROS";
        }
        return category.trim().toUpperCase();
    }
}
