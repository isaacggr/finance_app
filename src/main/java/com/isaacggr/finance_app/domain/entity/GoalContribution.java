package com.isaacggr.finance_app.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(name = "goal_contributions", indexes = {
        @Index(name = "idx_goal_contributions_goal_date", columnList = "goal_id, contribution_date")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoalContribution {

    public static final ZoneId DEFAULT_ZONE = ZoneId.of("America/Sao_Paulo");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goal_id", nullable = false)
    private Goal goal;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "contribution_date", nullable = false)
    private LocalDate contributionDate;

    @Column(length = 255)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public GoalContribution(Goal goal, BigDecimal amount, LocalDate contributionDate, String note) {
        BigDecimal normalizedAmount = Transaction.normalizeMonetaryAmount(amount);
        if (normalizedAmount == null || normalizedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor do aporte deve ser maior que zero");
        }
        this.goal = goal;
        this.amount = normalizedAmount;
        this.contributionDate = (contributionDate != null) ? contributionDate : LocalDate.now(DEFAULT_ZONE);
        this.note = Goal.normalizeOptionalText(note);
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
