package com.isaacggr.finance_app.service;

import com.isaacggr.finance_app.domain.entity.Budget;
import com.isaacggr.finance_app.domain.entity.Transaction;
import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.domain.enums.TransactionType;
import com.isaacggr.finance_app.dto.BudgetCreateDTO;
import com.isaacggr.finance_app.dto.BudgetProgressDTO;
import com.isaacggr.finance_app.dto.BudgetResponseDTO;
import com.isaacggr.finance_app.dto.BudgetUpdateDTO;
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
    private final BudgetAlertEvaluator budgetAlertEvaluator;

    public BudgetService(
            BudgetRepository budgetRepository,
            TransactionRepository transactionRepository,
            BudgetAlertEvaluator budgetAlertEvaluator
    ) {
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
        this.budgetAlertEvaluator = budgetAlertEvaluator;
    }

    // ==========================================
    // CREATE (Com validação estrita de unicidade)
    // ==========================================
    public BudgetResponseDTO create(UUID userId, User user, BudgetCreateDTO dto) {
        String normalizedCategory = Transaction.normalizeCategory(dto.category());

        boolean exists = budgetRepository.existsByUserIdAndCategoryAndMonthAndYear(
                userId,
                normalizedCategory,
                dto.month(),
                dto.year()
        );

        if (exists) {
            throw new ConflictException("Já existe um orçamento ativo para esta categoria neste mês/ano");
        }

        Budget budget = new Budget(
                user,
                normalizedCategory,
                dto.amount(),
                dto.month(),
                dto.year()
        );

        Budget saved = budgetRepository.save(budget);
        budgetAlertEvaluator.evaluateBudget(saved.getUser(), saved);
        return BudgetResponseDTO.fromEntity(saved);
    }

    // ==========================================
    // LIST BY PERIOD
    // ==========================================
    @Transactional(readOnly = true)
    public List<BudgetResponseDTO> listByPeriod(UUID userId, Integer month, Integer year) {
        int effectiveMonth = (month != null) ? month : LocalDate.now().getMonthValue();
        int effectiveYear = (year != null) ? year : LocalDate.now().getYear();

        return budgetRepository.findByUserIdAndMonthAndYear(userId, effectiveMonth, effectiveYear)
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
        List<Budget> budgets = budgetRepository.findByUserIdAndMonthAndYear(userId, effectiveMonth, effectiveYear);

        // 2. Busca todas as despesas reais agrupadas por categoria em 1 única query rápida no MySQL
        List<CategoryExpenseDTO> expenses = transactionRepository.sumAmountByCategory(
                userId,
                TransactionType.EXPENSE,
                startDate,
                endDate
        );

        Map<String, BigDecimal> spentMap = expenses.stream()
                .collect(Collectors.toMap(CategoryExpenseDTO::category, CategoryExpenseDTO::total));

        // 3. Monta o DTO de progresso calculando o percentual consumido e se estourou a meta
        return budgets.stream().map(budget -> {
            BigDecimal spent = Transaction.normalizeMonetaryAmount(
                    spentMap.getOrDefault(budget.getCategory(), BigDecimal.ZERO)
            );
            BigDecimal remaining = Transaction.normalizeMonetaryAmount(budget.getAmount().subtract(spent));

            BigDecimal percentageUsed = Transaction.normalizeMonetaryAmount(BigDecimal.ZERO);
            if (budget.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                percentageUsed = spent.multiply(BigDecimal.valueOf(100))
                        .divide(budget.getAmount(), 2, RoundingMode.HALF_EVEN);
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
    // UPDATE (Atualiza orçamento sem IDOR e validando unicidade)
    // ==========================================
    public BudgetResponseDTO update(UUID id, UUID userId, BudgetUpdateDTO dto) {
        Budget budget = budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Orçamento não encontrado"));

        String targetCategory = (dto.category() != null)
                ? Transaction.normalizeCategory(dto.category())
                : budget.getCategory();
        int targetMonth = (dto.month() != null) ? dto.month() : budget.getMonth();
        int targetYear = (dto.year() != null) ? dto.year() : budget.getYear();

        boolean keyChanged = !targetCategory.equals(budget.getCategory())
                || targetMonth != budget.getMonth()
                || targetYear != budget.getYear();

        if (keyChanged) {
            boolean duplicate = budgetRepository.existsByUserIdAndCategoryAndMonthAndYearAndIdNot(
                    userId,
                    targetCategory,
                    targetMonth,
                    targetYear,
                    id
            );

            if (duplicate) {
                throw new ConflictException("Já existe um orçamento ativo para esta categoria neste mês/ano");
            }
        }

        budget.update(targetCategory, dto.amount(), targetMonth, targetYear);
        Budget saved = budgetRepository.save(budget);
        budgetAlertEvaluator.evaluateBudget(saved.getUser(), saved);
        return BudgetResponseDTO.fromEntity(saved);
    }

    // ==========================================
    // DELETE (Hard Delete definitivo com proteção Zero IDOR)
    // ==========================================
    public void delete(UUID id, UUID userId) {
        Budget budget = budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Orçamento não encontrado"));

        budgetRepository.delete(budget);
    }
}
