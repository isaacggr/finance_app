package com.isaacggr.goldforge.domain.entity;

import com.isaacggr.goldforge.domain.enums.TransactionType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_transactions_user_date", columnList = "user_id, date"),
        @Index(name = "idx_transactions_installment_group", columnList = "installment_group_id")
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "installment_group_id")
    private InstallmentGroup installmentGroup;

    @Column(name = "installment_number")
    private Integer installmentNumber;

    @Column(name = "total_installments")
    private Integer totalInstallments;

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
    // CONSTRUTORES
    // =========================
    public Transaction(
            User user,
            BigDecimal amount,
            TransactionType type,
            String category,
            LocalDate date,
            String description) {

        this(user, amount, type, category, date, description, null, null, null);
    }

    public Transaction(
            User user,
            BigDecimal amount,
            TransactionType type,
            String category,
            LocalDate date,
            String description,
            InstallmentGroup installmentGroup,
            Integer installmentNumber,
            Integer totalInstallments) {

        this.user = user;
        this.amount = normalizeMonetaryAmount(amount);
        this.type = type;
        this.category = normalizeCategory(category);
        this.date = date;
        this.description = description;
        this.installmentGroup = installmentGroup;
        this.installmentNumber = installmentNumber;
        this.totalInstallments = totalInstallments;
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

        this.amount = normalizeMonetaryAmount(amount);
        this.type = type;
        this.category = normalizeCategory(category);
        this.date = date;
        this.description = description;
    }

    public UUID getInstallmentGroupId() {
        return this.installmentGroup != null ? this.installmentGroup.getId() : null;
    }

    public Integer getTotalInstallments() {
        if (this.totalInstallments != null) {
            return this.totalInstallments;
        }
        return this.installmentGroup != null ? this.installmentGroup.getTotalInstallments() : null;
    }

    public boolean isInstallment() {
        return this.installmentGroup != null;
    }

    public static BigDecimal normalizeMonetaryAmount(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        return amount.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return "OUTROS";
        }
        return category.trim().toUpperCase(Locale.of("pt", "BR"));
    }
}
