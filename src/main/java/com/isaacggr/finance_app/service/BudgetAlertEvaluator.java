package com.isaacggr.finance_app.service;

import com.isaacggr.finance_app.domain.entity.Budget;
import com.isaacggr.finance_app.domain.entity.Transaction;
import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.domain.enums.TransactionType;
import com.isaacggr.finance_app.repository.BudgetRepository;
import com.isaacggr.finance_app.repository.TransactionRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * Avalia em tempo real se o orçamento mensal de uma categoria atingiu o limite de alerta (80%)
 * ou foi ultrapassado (> 100%), evitando dependência circular entre TransactionService e BudgetService.
 */
@Component
public class BudgetAlertEvaluator {

    private static final BigDecimal WARNING_PERCENTAGE_THRESHOLD = new BigDecimal("80.00");

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationService notificationService;

    public BudgetAlertEvaluator(
            BudgetRepository budgetRepository,
            TransactionRepository transactionRepository,
            NotificationService notificationService
    ) {
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
        this.notificationService = notificationService;
    }

    public void evaluateFor(User user, String rawCategory, LocalDate referenceDate) {
        if (user == null || user.getId() == null || referenceDate == null) {
            return;
        }

        String normalizedCategory = Transaction.normalizeCategory(rawCategory);
        int month = referenceDate.getMonthValue();
        int year = referenceDate.getYear();

        budgetRepository.findByUserIdAndCategoryAndMonthAndYear(user.getId(), normalizedCategory, month, year)
                .ifPresent(budget -> evaluateBudget(user, budget));
    }

    public void evaluateBudget(User user, Budget budget) {
        if (user == null || budget == null || budget.getAmount() == null
                || budget.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        LocalDate startDate = LocalDate.of(budget.getYear(), budget.getMonth(), 1);
        LocalDate endDate = startDate.with(TemporalAdjusters.lastDayOfMonth());

        BigDecimal spent = transactionRepository.sumAmountByTypeAndCategoryAndPeriod(
                user.getId(),
                TransactionType.EXPENSE,
                budget.getCategory(),
                startDate,
                endDate
        );
        BigDecimal safeSpent = (spent != null)
                ? Transaction.normalizeMonetaryAmount(spent)
                : Transaction.normalizeMonetaryAmount(BigDecimal.ZERO);

        BigDecimal percentageUsed = safeSpent.multiply(BigDecimal.valueOf(100))
                .divide(budget.getAmount(), 2, RoundingMode.HALF_EVEN);

        boolean exceeded = safeSpent.compareTo(budget.getAmount()) > 0;
        if (exceeded || percentageUsed.compareTo(WARNING_PERCENTAGE_THRESHOLD) >= 0) {
            notificationService.notifyBudgetThreshold(user, budget, safeSpent, percentageUsed, exceeded);
        }
    }
}
