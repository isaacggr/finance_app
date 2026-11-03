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

        sampleBudget = new Budget(sampleUser, TransactionCategory.ALIMENTACAO, new BigDecimal("800.00"), 11, 2026);
        ReflectionTestUtils.setField(sampleBudget, "id", budgetId);
    }

    @Test
    @DisplayName("Deve criar um orçamento com sucesso")
    void create_Success() {
        BudgetCreateDTO dto = new BudgetCreateDTO(TransactionCategory.ALIMENTACAO, new BigDecimal("800.00"), 11, 2026);

        when(budgetRepository.existsByUserIdAndCategoryAndMonthAndYearAndDeletedFalse(userId, dto.category(), dto.month(), dto.year()))
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
        BudgetCreateDTO dto = new BudgetCreateDTO(TransactionCategory.ALIMENTACAO, new BigDecimal("800.00"), 11, 2026);

        when(budgetRepository.existsByUserIdAndCategoryAndMonthAndYearAndDeletedFalse(userId, dto.category(), dto.month(), dto.year()))
                .thenReturn(true);

        assertThrows(ConflictException.class, () -> budgetService.create(userId, sampleUser, dto));
        verify(budgetRepository, never()).save(any(Budget.class));
    }

    @Test
    @DisplayName("Deve listar orçamentos do período com sucesso")
    void listByPeriod_Success() {
        when(budgetRepository.findByUserIdAndMonthAndYearAndDeletedFalse(userId, 11, 2026))
                .thenReturn(List.of(sampleBudget));

        List<BudgetResponseDTO> result = budgetService.listByPeriod(userId, 11, 2026);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(sampleBudget.getCategory(), result.getFirst().category());
    }

    @Test
    @DisplayName("Deve calcular progresso do orçamento dentro do limite com sucesso")
    void getProgress_Success_WithinBudget() {
        when(budgetRepository.findByUserIdAndMonthAndYearAndDeletedFalse(userId, 11, 2026))
                .thenReturn(List.of(sampleBudget));

        List<CategoryExpenseDTO> expenses = List.of(
                new CategoryExpenseDTO(TransactionCategory.ALIMENTACAO, new BigDecimal("400.00"))
        );
        when(transactionRepository.sumAmountByCategory(eq(userId), eq(TransactionType.EXPENSE), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(expenses);

        List<BudgetProgressDTO> progressList = budgetService.getProgress(userId, 11, 2026);

        assertNotNull(progressList);
        assertEquals(1, progressList.size());

        BudgetProgressDTO progress = progressList.getFirst();
        assertEquals(TransactionCategory.ALIMENTACAO, progress.category());
        assertEquals(new BigDecimal("800.00"), progress.budgetAmount());
        assertEquals(new BigDecimal("400.00"), progress.spentAmount());
        assertEquals(new BigDecimal("400.00"), progress.remainingAmount());
        assertEquals(new BigDecimal("50.00"), progress.percentageUsed());
        assertFalse(progress.exceeded());
    }

    @Test
    @DisplayName("Deve calcular progresso do orçamento quando estourar o limite")
    void getProgress_Success_ExceededBudget() {
        when(budgetRepository.findByUserIdAndMonthAndYearAndDeletedFalse(userId, 11, 2026))
                .thenReturn(List.of(sampleBudget));

        List<CategoryExpenseDTO> expenses = List.of(
                new CategoryExpenseDTO(TransactionCategory.ALIMENTACAO, new BigDecimal("1000.00"))
        );
        when(transactionRepository.sumAmountByCategory(eq(userId), eq(TransactionType.EXPENSE), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(expenses);

        List<BudgetProgressDTO> progressList = budgetService.getProgress(userId, 11, 2026);

        assertNotNull(progressList);
        assertEquals(1, progressList.size());

        BudgetProgressDTO progress = progressList.getFirst();
        assertEquals(new BigDecimal("800.00"), progress.budgetAmount());
        assertEquals(new BigDecimal("1000.00"), progress.spentAmount());
        assertEquals(new BigDecimal("-200.00"), progress.remainingAmount());
        assertEquals(new BigDecimal("125.00"), progress.percentageUsed());
        assertTrue(progress.exceeded());
    }

    @Test
    @DisplayName("Deve excluir logicamente o orçamento com sucesso")
    void delete_Success() {
        when(budgetRepository.findByIdAndUserIdAndDeletedFalse(budgetId, userId))
                .thenReturn(Optional.of(sampleBudget));

        budgetService.delete(budgetId, userId);

        assertTrue(sampleBudget.isDeleted());
    }

    @Test
    @DisplayName("Deve lançar NotFoundException ao tentar excluir orçamento inexistente ou de outro usuário")
    void delete_ThrowsNotFound_WhenNotFoundOrNotOwned() {
        when(budgetRepository.findByIdAndUserIdAndDeletedFalse(budgetId, userId))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> budgetService.delete(budgetId, userId));
    }
}

