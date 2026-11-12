package com.isaacggr.finance_app.service;

import com.isaacggr.finance_app.domain.entity.Budget;
import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.domain.enums.TransactionType;
import com.isaacggr.finance_app.repository.BudgetRepository;
import com.isaacggr.finance_app.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetAlertEvaluatorTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private BudgetAlertEvaluator evaluator;

    private User user;
    private UUID userId;
    private Budget budget;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new User("Isaac Gregorio", "isaac@example.com", "hash");
        ReflectionTestUtils.setField(user, "id", userId);

        budget = new Budget(user, "ALIMENTAÇÃO", new BigDecimal("1000.00"), 11, 2026);
        ReflectionTestUtils.setField(budget, "id", UUID.randomUUID());
    }

    @Test
    @DisplayName("Não deve notificar quando gasto do orçamento estiver abaixo de 80%")
    void evaluateFor_DoesNotNotifyBelowWarningThreshold() {
        when(budgetRepository.findByUserIdAndCategoryAndMonthAndYear(userId, "ALIMENTAÇÃO", 11, 2026))
                .thenReturn(Optional.of(budget));
        when(transactionRepository.sumAmountByTypeAndCategoryAndPeriod(
                userId, TransactionType.EXPENSE, "ALIMENTAÇÃO",
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)))
                .thenReturn(new BigDecimal("790.00"));

        evaluator.evaluateFor(user, "alimentação", LocalDate.of(2026, 11, 15));

        verify(notificationService, never()).notifyBudgetThreshold(any(), any(), any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("Deve notificar alerta preventivo quando consumo atingir entre 80% e 100%")
    void evaluateFor_NotifiesWarningBetween80And100Percent() {
        when(budgetRepository.findByUserIdAndCategoryAndMonthAndYear(userId, "ALIMENTAÇÃO", 11, 2026))
                .thenReturn(Optional.of(budget));
        when(transactionRepository.sumAmountByTypeAndCategoryAndPeriod(
                userId, TransactionType.EXPENSE, "ALIMENTAÇÃO",
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)))
                .thenReturn(new BigDecimal("850.00"));

        evaluator.evaluateFor(user, "ALIMENTAÇÃO", LocalDate.of(2026, 11, 15));

        verify(notificationService).notifyBudgetThreshold(
                eq(user), eq(budget), eq(new BigDecimal("850.00")), eq(new BigDecimal("85.00")), eq(false));
    }

    @Test
    @DisplayName("Deve notificar estouro quando consumo ultrapassar 100% do orçamento")
    void evaluateFor_NotifiesExceededAbove100Percent() {
        when(budgetRepository.findByUserIdAndCategoryAndMonthAndYear(userId, "ALIMENTAÇÃO", 11, 2026))
                .thenReturn(Optional.of(budget));
        when(transactionRepository.sumAmountByTypeAndCategoryAndPeriod(
                userId, TransactionType.EXPENSE, "ALIMENTAÇÃO",
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)))
                .thenReturn(new BigDecimal("1200.00"));

        evaluator.evaluateFor(user, "ALIMENTAÇÃO", LocalDate.of(2026, 11, 15));

        verify(notificationService).notifyBudgetThreshold(
                eq(user), eq(budget), eq(new BigDecimal("1200.00")), eq(new BigDecimal("120.00")), eq(true));
    }
}
