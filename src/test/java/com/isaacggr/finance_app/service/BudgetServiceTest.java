package com.isaacggr.finance_app.service;

import com.isaacggr.finance_app.domain.entity.Budget;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BudgetAlertEvaluator budgetAlertEvaluator;

    @InjectMocks
    private BudgetService budgetService;

    private User sampleUser;
    private UUID userId;
    private UUID budgetId;
    private Budget sampleBudget;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        budgetId = UUID.randomUUID();
        sampleUser = new User("Isaac Gregorio", "isaac@example.com", "hash123");
        ReflectionTestUtils.setField(sampleUser, "id", userId);

        sampleBudget = new Budget(sampleUser, "ALIMENTACAO", new BigDecimal("800.00"), 11, 2026);
        ReflectionTestUtils.setField(sampleBudget, "id", budgetId);
    }

    @Test
    @DisplayName("Deve criar um orçamento com sucesso")
    void create_Success() {
        BudgetCreateDTO dto = new BudgetCreateDTO("ALIMENTACAO", new BigDecimal("800.00"), 11, 2026);

        when(budgetRepository.existsByUserIdAndCategoryAndMonthAndYear(userId, "ALIMENTACAO", dto.month(), dto.year()))
                .thenReturn(false);
        when(budgetRepository.save(any(Budget.class))).thenReturn(sampleBudget);

        BudgetResponseDTO response = budgetService.create(userId, sampleUser, dto);

        assertNotNull(response);
        assertEquals(dto.category(), response.category());
        assertEquals(new BigDecimal("800.00"), response.amount());
        assertEquals(11, response.month());
        assertEquals(2026, response.year());
        verify(budgetRepository, times(1)).save(any(Budget.class));
    }

    @Test
    @DisplayName("Deve lançar ConflictException quando já existir orçamento para a categoria no mês/ano")
    void create_ThrowsConflictException_WhenAlreadyExists() {
        BudgetCreateDTO dto = new BudgetCreateDTO("ALIMENTACAO", new BigDecimal("800.00"), 11, 2026);

        when(budgetRepository.existsByUserIdAndCategoryAndMonthAndYear(userId, "ALIMENTACAO", dto.month(), dto.year()))
                .thenReturn(true);

        assertThrows(ConflictException.class, () -> budgetService.create(userId, sampleUser, dto));
        verify(budgetRepository, never()).save(any(Budget.class));
    }

    @Test
    @DisplayName("Deve listar orçamentos do período com sucesso")
    void listByPeriod_Success() {
        when(budgetRepository.findByUserIdAndMonthAndYear(userId, 11, 2026))
                .thenReturn(List.of(sampleBudget));

        List<BudgetResponseDTO> result = budgetService.listByPeriod(userId, 11, 2026);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(sampleBudget.getCategory(), result.getFirst().category());
    }

    @Test
    @DisplayName("Deve calcular progresso do orçamento dentro do limite, excedido e sem gastos")
    void getProgress_CalculatesWithinExceededAndZeroSpentBudgets() {
        assertBudgetProgress(List.of(new CategoryExpenseDTO("ALIMENTACAO", new BigDecimal("400.00"))),
                "400.00", "400.00", "50.00", false);
        assertBudgetProgress(List.of(new CategoryExpenseDTO("ALIMENTACAO", new BigDecimal("1000.00"))),
                "1000.00", "-200.00", "125.00", true);
        assertBudgetProgress(List.of(), "0.00", "800.00", "0.00", false);
    }

    private void assertBudgetProgress(
            List<CategoryExpenseDTO> expenses,
            String expectedSpent,
            String expectedRemaining,
            String expectedPercentage,
            boolean expectedExceeded
    ) {
        when(budgetRepository.findByUserIdAndMonthAndYear(userId, 11, 2026))
                .thenReturn(List.of(sampleBudget));
        when(transactionRepository.sumAmountByCategory(eq(userId), eq(TransactionType.EXPENSE), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(expenses);

        List<BudgetProgressDTO> progressList = budgetService.getProgress(userId, 11, 2026);

        assertEquals(1, progressList.size());
        BudgetProgressDTO progress = progressList.getFirst();
        assertEquals("ALIMENTACAO", progress.category());
        assertEquals(new BigDecimal("800.00"), progress.budgetAmount());
        assertEquals(new BigDecimal(expectedSpent), progress.spentAmount());
        assertEquals(new BigDecimal(expectedRemaining), progress.remainingAmount());
        assertEquals(new BigDecimal(expectedPercentage), progress.percentageUsed());
        assertEquals(expectedExceeded, progress.exceeded());
    }

    @Test
    @DisplayName("Deve atualizar apenas o valor do orçamento existente mantendo categoria e período")
    void update_Success_AmountOnly() {
        BudgetUpdateDTO dto = new BudgetUpdateDTO(null, new BigDecimal("1200.0"), null, null);
        when(budgetRepository.findByIdAndUserId(budgetId, userId))
                .thenReturn(Optional.of(sampleBudget));
        when(budgetRepository.save(sampleBudget)).thenReturn(sampleBudget);

        BudgetResponseDTO response = budgetService.update(budgetId, userId, dto);

        assertEquals("ALIMENTACAO", response.category());
        assertEquals(new BigDecimal("1200.00"), response.amount());
        assertEquals(11, response.month());
        assertEquals(2026, response.year());
        verify(budgetRepository, never()).existsByUserIdAndCategoryAndMonthAndYearAndIdNot(any(), any(), anyInt(), anyInt(), any());
    }

    @Test
    @DisplayName("Deve atualizar categoria e período do orçamento quando não houver conflito")
    void update_Success_ChangingCategoryAndPeriod() {
        BudgetUpdateDTO dto = new BudgetUpdateDTO("transporte", new BigDecimal("550.00"), 12, 2026);
        when(budgetRepository.findByIdAndUserId(budgetId, userId))
                .thenReturn(Optional.of(sampleBudget));
        when(budgetRepository.existsByUserIdAndCategoryAndMonthAndYearAndIdNot(
                userId, "TRANSPORTE", 12, 2026, budgetId))
                .thenReturn(false);
        when(budgetRepository.save(sampleBudget)).thenReturn(sampleBudget);

        BudgetResponseDTO response = budgetService.update(budgetId, userId, dto);

        assertEquals("TRANSPORTE", response.category());
        assertEquals(new BigDecimal("550.00"), response.amount());
        assertEquals(12, response.month());
        assertEquals(2026, response.year());
        verify(budgetRepository).save(sampleBudget);
    }

    @Test
    @DisplayName("Deve lançar ConflictException ao atualizar orçamento para chave já ocupada por outro orçamento")
    void update_ThrowsConflict_WhenTargetKeyAlreadyExists() {
        BudgetUpdateDTO dto = new BudgetUpdateDTO("transporte", new BigDecimal("500.00"), 11, 2026);
        when(budgetRepository.findByIdAndUserId(budgetId, userId))
                .thenReturn(Optional.of(sampleBudget));
        when(budgetRepository.existsByUserIdAndCategoryAndMonthAndYearAndIdNot(
                userId, "TRANSPORTE", 11, 2026, budgetId))
                .thenReturn(true);

        assertThrows(ConflictException.class, () -> budgetService.update(budgetId, userId, dto));
        verify(budgetRepository, never()).save(any(Budget.class));
    }

    @Test
    @DisplayName("Deve lançar NotFoundException ao tentar atualizar orçamento inexistente ou de outro usuário")
    void update_ThrowsNotFound_WhenNotFoundOrNotOwned() {
        BudgetUpdateDTO dto = new BudgetUpdateDTO(null, new BigDecimal("500.00"), null, null);
        when(budgetRepository.findByIdAndUserId(budgetId, userId))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> budgetService.update(budgetId, userId, dto));
    }

    @Test
    @DisplayName("Deve excluir fisicamente o orçamento com sucesso (Hard Delete)")
    void delete_Success() {
        when(budgetRepository.findByIdAndUserId(budgetId, userId))
                .thenReturn(Optional.of(sampleBudget));

        budgetService.delete(budgetId, userId);

        verify(budgetRepository, times(1)).delete(sampleBudget);
    }

    @Test
    @DisplayName("Deve lançar NotFoundException ao tentar excluir orçamento inexistente ou de outro usuário")
    void delete_ThrowsNotFound_WhenNotFoundOrNotOwned() {
        when(budgetRepository.findByIdAndUserId(budgetId, userId))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> budgetService.delete(budgetId, userId));
    }
}
