package com.isaacggr.finance_app.domain.entity;

import com.isaacggr.finance_app.domain.enums.TransactionCategory;
import com.isaacggr.finance_app.domain.enums.TransactionType;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "transactions")
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionCategory category;

    @Column(nullable = false)
    private LocalDate date;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private boolean deleted;

    // =========================
    // CONSTRUTOR
    // =========================
    public Transaction(
            User user,
            BigDecimal amount,
            TransactionType type,
            TransactionCategory category,
            LocalDate date,
            String description) {

        this.user = user;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.date = date;
        this.description = description;
        this.deleted = false;
    }

    // =========================
    // MUDANÇA DE ESTADO
    // =========================
    public void update(
            BigDecimal amount,
            TransactionType type,
            TransactionCategory category,
            LocalDate date,
            String description) {

        this.amount = amount;
        this.type = type;
        this.category = category;
        this.date = date;
        this.description = description;
    }

    public void markAsDeleted() {
        this.deleted = true;
    }
}
