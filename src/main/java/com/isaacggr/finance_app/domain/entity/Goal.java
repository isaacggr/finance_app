package com.isaacggr.finance_app.domain.entity;

import com.isaacggr.finance_app.domain.enums.GoalStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "goals", indexes = {
        @Index(name = "idx_goals_user_primary", columnList = "user_id, is_primary")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 80)
    private String title;

    @Column(length = 255)
    private String description;

    @Column(name = "target_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal targetAmount;

    @Column(name = "current_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal currentAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);

    @Column
    private LocalDate deadline;

    @Column(name = "is_primary", nullable = false)
    private boolean primary = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Goal(
            User user,
            String title,
            String description,
            BigDecimal targetAmount,
            LocalDate deadline,
            boolean primary
    ) {
        this.user = user;
        this.title = normalizeRequiredTitle(title);
        this.description = normalizeOptionalText(description);
        this.targetAmount = normalizePositiveAmount(targetAmount, "O valor alvo da meta deve ser maior que zero");
        this.currentAmount = Transaction.normalizeMonetaryAmount(BigDecimal.ZERO);
        this.deadline = deadline;
        this.primary = primary;
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

    public void update(
            String title,
            String description,
            BigDecimal targetAmount,
            LocalDate deadline,
            boolean primary
    ) {
        this.title = normalizeRequiredTitle(title);
        this.description = normalizeOptionalText(description);
        this.targetAmount = normalizePositiveAmount(targetAmount, "O valor alvo da meta deve ser maior que zero");
        this.deadline = deadline;
        this.primary = primary;
    }

    public void markAsPrimary() {
        this.primary = true;
    }

    public void unmarkAsPrimary() {
        this.primary = false;
    }

    public void applyContribution(BigDecimal amount) {
        BigDecimal normalized = normalizePositiveAmount(amount, "O valor do aporte deve ser maior que zero");
        this.currentAmount = Transaction.normalizeMonetaryAmount(this.currentAmount.add(normalized));
    }

    public void revertContribution(BigDecimal amount) {
        BigDecimal normalized = normalizePositiveAmount(amount, "O valor do aporte deve ser maior que zero");
        BigDecimal updated = this.currentAmount.subtract(normalized);
        this.currentAmount = updated.compareTo(BigDecimal.ZERO) > 0
                ? Transaction.normalizeMonetaryAmount(updated)
                : Transaction.normalizeMonetaryAmount(BigDecimal.ZERO);
    }

    public BigDecimal getRemainingAmount() {
        BigDecimal remaining = this.targetAmount.subtract(this.currentAmount);
        return remaining.compareTo(BigDecimal.ZERO) > 0
                ? Transaction.normalizeMonetaryAmount(remaining)
                : Transaction.normalizeMonetaryAmount(BigDecimal.ZERO);
    }

    /**
     * Calcula o percentual concluído em escala 2 com arredondamento bancário (HALF_EVEN).
     */
    public BigDecimal getPercentage() {
        if (this.targetAmount == null || this.targetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return Transaction.normalizeMonetaryAmount(BigDecimal.ZERO);
        }
        return this.currentAmount
                .multiply(BigDecimal.valueOf(100))
                .divide(this.targetAmount, 2, RoundingMode.HALF_EVEN);
    }

    public GoalStatus getStatus() {
        return this.currentAmount.compareTo(this.targetAmount) >= 0
                ? GoalStatus.COMPLETED
                : GoalStatus.IN_PROGRESS;
    }

    public boolean isCompleted() {
        return getStatus() == GoalStatus.COMPLETED;
    }

    private static String normalizeRequiredTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("O título da meta é obrigatório");
        }
        return title.trim();
    }

    private static BigDecimal normalizePositiveAmount(BigDecimal amount, String message) {
        BigDecimal normalized = Transaction.normalizeMonetaryAmount(amount);
        if (normalized == null || normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    public static String normalizeOptionalText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return text.trim();
    }
}
