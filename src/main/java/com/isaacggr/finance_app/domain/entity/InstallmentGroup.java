package com.isaacggr.finance_app.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "installment_groups", indexes = {
        @Index(name = "idx_installment_groups_user", columnList = "user_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InstallmentGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "total_installments", nullable = false)
    private Integer totalInstallments;

    @Column(nullable = false, length = 30)
    private String category;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @OneToMany(mappedBy = "installmentGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("installmentNumber ASC")
    private List<Bill> bills = new ArrayList<>();

    @OneToMany(mappedBy = "installmentGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("installmentNumber ASC")
    private List<Transaction> transactions = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public InstallmentGroup(
            User user,
            String description,
            BigDecimal totalAmount,
            Integer totalInstallments,
            String category,
            LocalDate startDate
    ) {
        if (user == null) {
            throw new IllegalArgumentException("O usuário é obrigatório");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("A descrição da compra parcelada é obrigatória");
        }
        if (totalInstallments == null || totalInstallments < 2) {
            throw new IllegalArgumentException("O número total de parcelas deve ser de no mínimo 2");
        }
        if (startDate == null) {
            throw new IllegalArgumentException("A data da primeira parcela é obrigatória");
        }

        this.user = user;
        this.description = description.trim();
        this.totalAmount = Transaction.normalizeMonetaryAmount(totalAmount);
        this.totalInstallments = totalInstallments;
        this.category = Transaction.normalizeCategory(category);
        this.startDate = startDate;
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
}
