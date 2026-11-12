package com.isaacggr.finance_app.domain.entity;

import com.isaacggr.finance_app.domain.enums.BillRecurrence;
import com.isaacggr.finance_app.domain.enums.BillStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "bills", indexes = {
        @Index(name = "idx_bills_user_status_due", columnList = "user_id, status, due_date"),
        @Index(name = "idx_bills_status_due", columnList = "status, due_date")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bill {

    public static final ZoneId DEFAULT_ZONE = ZoneId.of("America/Sao_Paulo");

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

    @Column(nullable = false, length = 30)
    private String category;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BillRecurrence recurrence = BillRecurrence.ONCE;

    // Persistido apenas como PENDING ou PAID; OVERDUE é derivado em tempo real comparando dueDate com hoje.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BillStatus status = BillStatus.PENDING;

    @Column(name = "paid_at")
    private LocalDate paidAt;

    @Column(name = "paid_transaction_id")
    private UUID paidTransactionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Bill(
            User user,
            String title,
            String description,
            String category,
            BigDecimal amount,
            LocalDate dueDate,
            BillRecurrence recurrence
    ) {
        this.user = user;
        this.title = normalizeRequiredTitle(title);
        this.description = Goal.normalizeOptionalText(description);
        this.category = Transaction.normalizeCategory(category);
        this.amount = normalizePositiveAmount(amount);
        this.dueDate = requireDueDate(dueDate);
        this.recurrence = (recurrence != null) ? recurrence : BillRecurrence.ONCE;
        this.status = BillStatus.PENDING;
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
            String category,
            BigDecimal amount,
            LocalDate dueDate,
            BillRecurrence recurrence
    ) {
        this.title = normalizeRequiredTitle(title);
        this.description = Goal.normalizeOptionalText(description);
        this.category = Transaction.normalizeCategory(category);
        this.amount = normalizePositiveAmount(amount);
        this.dueDate = requireDueDate(dueDate);
        this.recurrence = (recurrence != null) ? recurrence : BillRecurrence.ONCE;
    }

    public void markAsPaid(LocalDate paidDate, UUID paidTransactionId) {
        this.status = BillStatus.PAID;
        this.paidAt = (paidDate != null) ? paidDate : LocalDate.now(DEFAULT_ZONE);
        this.paidTransactionId = paidTransactionId;
    }

    public boolean isPaid() {
        return this.status == BillStatus.PAID;
    }

    public BillStatus getStatus() {
        return getStatusOn(LocalDate.now(DEFAULT_ZONE));
    }

    /**
     * Deriva OVERDUE dinamicamente para contas pendentes já vencidas sem depender de job noturno no banco.
     */
    public BillStatus getStatusOn(LocalDate referenceDate) {
        LocalDate today = (referenceDate != null) ? referenceDate : LocalDate.now(DEFAULT_ZONE);
        if (this.status == BillStatus.PENDING && this.dueDate != null && this.dueDate.isBefore(today)) {
            return BillStatus.OVERDUE;
        }
        return this.status;
    }

    public long getDueInDays() {
        return getDueInDaysOn(LocalDate.now(DEFAULT_ZONE));
    }

    public long getDueInDaysOn(LocalDate referenceDate) {
        LocalDate today = (referenceDate != null) ? referenceDate : LocalDate.now(DEFAULT_ZONE);
        return ChronoUnit.DAYS.between(today, this.dueDate);
    }

    public Optional<LocalDate> nextDueDate() {
        return switch (this.recurrence) {
            case MONTHLY -> Optional.of(this.dueDate.plusMonths(1));
            case YEARLY -> Optional.of(this.dueDate.plusYears(1));
            case ONCE -> Optional.empty();
        };
    }

    public Optional<Bill> createNextRecurringBill() {
        return nextDueDate().map(nextDate -> new Bill(
                this.user,
                this.title,
                this.description,
                this.category,
                this.amount,
                nextDate,
                this.recurrence
        ));
    }

    private static String normalizeRequiredTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("O título da conta é obrigatório");
        }
        return title.trim();
    }

    private static BigDecimal normalizePositiveAmount(BigDecimal amount) {
        BigDecimal normalized = Transaction.normalizeMonetaryAmount(amount);
        if (normalized == null || normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor da conta deve ser maior que zero");
        }
        return normalized;
    }

    private static LocalDate requireDueDate(LocalDate dueDate) {
        if (dueDate == null) {
            throw new IllegalArgumentException("A data de vencimento é obrigatória");
        }
        return dueDate;
    }
}
