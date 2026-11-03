package com.isaacggr.finance_app.service;

import com.isaacggr.finance_app.domain.entity.Budget;
import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.domain.enums.TransactionCategory;
import com.isaacggr.finance_app.domain.enums.TransactionType;
import com.isaacggr.finance_app.dto.BudgetCreateDTO;
import com.isaacggr.finance_app.dto.BudgetProgressDTO;
import com.isaacggr.finance_app.dto.BudgetResponseDTO;
import com.isaacggr.finance_app.dto.CategoryExpenseDTO;
import com.isaacggr.finance_app.exception.ConflictException;
import com.isaacggr.finance_app.exception.NotFoundException;
import com.isaacggr.finance_app.repository.BudgetRepository;
import com.isaacggr.finance_app.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;

    public BudgetService(BudgetRepository budgetRepository, TransactionRepository transactionRepository) {
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
    }

    // ==========================================
    // CREATE (Com validação estrita de unicidade)
    // ==========================================
    public BudgetResponseDTO create(UUID userId, User user, BudgetCreateDTO dto) {
        boolean exists = budgetRepository.existsByUserIdAndCategoryAndMonthAndYearAndDeletedFalse(
                userId,
                dto.category(),
                dto.month(),
                dto.year()
        );

        if (exists) {
            throw new ConflictException("Já existe um orçamento ativo para esta categoria neste mês/ano");
        }

        Budget budget = new Budget(
                user,
                dto.category(),
                dto.amount(),
                dto.month(),
                dto.year()
        );

        return BudgetResponseDTO.fromEntity(budgetRepository.save(budget));
    }

    // ==========================================
    // LIST BY PERIOD
    // ==========================================
    @Transactional(readOnly = true)
    public List<BudgetResponseDTO> listByPeriod(UUID userId, Integer month, Integer year) {
        int effectiveMonth = (month != null) ? month : LocalDate.now().getMonthValue();
        int effectiveYear = (year != null) ? year : LocalDate.now().getYear();

        return budgetRepository.findByUserIdAndMonthAndYearAndDeletedFalse(userId, effectiveMonth, effectiveYear)
                .stream()
                .map(BudgetResponseDTO::fromEntity)
                .toList();
    }

    // ==========================================
    // GET PROGRESS (Cálculo de metas consumidas)
    // ==========================================
    @Transactional(readOnly = true)
    public List<BudgetProgressDTO> getProgress(UUID userId, Integer month, Integer year) {
        int effectiveMonth = (month != null) ? month : LocalDate.now().getMonthValue();
        int effectiveYear = (year != null) ? year : LocalDate.now().getYear();

        LocalDate startDate = LocalDate.of(effectiveYear, effectiveMonth, 1);
        LocalDate endDate = startDate.with(TemporalAdjusters.lastDayOfMonth());

        // 1. Busca todos os orçamentos definidos pelo usuário para o mês
        List<Budget> budgets = budgetRepository.findByUserIdAndMonthAndYearAndDeletedFalse(userId, effectiveMonth, effectiveYear);

        // 2. Busca todas as despesas reais agrupadas por categoria em 1 única query rápida no MySQL
        List<CategoryExpenseDTO> expenses = transactionRepository.sumAmountByCategory(
                userId,
                TransactionType.EXPENSE,
                startDate,
                endDate
        );

        Map<TransactionCategory, BigDecimal> spentMap = expenses.stream()
                .collect(Collectors.toMap(CategoryExpenseDTO::category, CategoryExpenseDTO::total));

        // 3. Monta o DTO de progresso calculando o percentual consumido e se estourou a meta
        return budgets.stream().map(budget -> {
            BigDecimal spent = spentMap.getOrDefault(budget.getCategory(), BigDecimal.ZERO);
            BigDecimal remaining = budget.getAmount().subtract(spent);

            BigDecimal percentageUsed = BigDecimal.ZERO;
            if (budget.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                percentageUsed = spent.multiply(BigDecimal.valueOf(100))
                        .divide(budget.getAmount(), 2, RoundingMode.HALF_UP);
            }

            boolean exceeded = spent.compareTo(budget.getAmount()) > 0;

            return new BudgetProgressDTO(
                    budget.getId(),
                    budget.getCategory(),
                    budget.getAmount(),
                    spent,
                    remaining,
                    percentageUsed,
                    exceeded
            );
        }).toList();
    }

    // ==========================================
    // DELETE (Soft delete com proteção Zero IDOR)
    // ==========================================
    public void delete(UUID id, UUID userId) {
        Budget budget = budgetRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(() -> new NotFoundException("Orçamento não encontrado"));

        budget.markAsDeleted();
    }
}

