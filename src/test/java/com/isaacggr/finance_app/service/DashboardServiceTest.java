package com.isaacggr.finance_app.service;

import com.isaacggr.finance_app.domain.enums.TransactionCategory;
import com.isaacggr.finance_app.domain.enums.TransactionType;
import com.isaacggr.finance_app.dto.CategoryExpenseDTO;
import com.isaacggr.finance_app.dto.DashboardSummaryDTO;
import com.isaacggr.finance_app.exception.ConflictException;
import com.isaacggr.finance_app.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Deve calcular resumo do dashboard com datas explícitas com sucesso")
    void getSummary_Success_WithExplicitDates() {
        LocalDate startDate = LocalDate.of(2026, 11, 1);
        LocalDate endDate = LocalDate.of(2026, 11, 30);

        when(transactionRepository.sumAmountByTypeAndPeriod(userId, TransactionType.INCOME, startDate, endDate))
                .thenReturn(new BigDecimal("5000.00"));
        when(transactionRepository.sumAmountByTypeAndPeriod(userId, TransactionType.EXPENSE, startDate, endDate))
                .thenReturn(new BigDecimal("2100.50"));

        List<CategoryExpenseDTO> categories = List.of(
                new CategoryExpenseDTO(TransactionCategory.ALIMENTACAO, new BigDecimal("1500.00")),
                new CategoryExpenseDTO(TransactionCategory.TRANSPORTE, new BigDecimal("600.50"))
        );
        when(transactionRepository.sumAmountByCategory(userId, TransactionType.EXPENSE, startDate, endDate))
                .thenReturn(categories);

        DashboardSummaryDTO summary = dashboardService.getSummary(userId, startDate, endDate);

        assertNotNull(summary);
        assertEquals(startDate, summary.startDate());
        assertEquals(endDate, summary.endDate());
        assertEquals(new BigDecimal("5000.00"), summary.totalIncome());
        assertEquals(new BigDecimal("2100.50"), summary.totalExpense());
        assertEquals(new BigDecimal("2899.50"), summary.balance());
        assertEquals(2, summary.expensesByCategory().size());
    }

    @Test
    @DisplayName("Deve calcular resumo do dashboard com datas padrão (mês corrente) quando omitidas")
    void getSummary_Success_WithDefaultDates() {
        LocalDate expectedStart = LocalDate.now().withDayOfMonth(1);
        LocalDate expectedEnd = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

        when(transactionRepository.sumAmountByTypeAndPeriod(eq(userId), eq(TransactionType.INCOME), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("3000.00"));
        when(transactionRepository.sumAmountByTypeAndPeriod(eq(userId), eq(TransactionType.EXPENSE), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("1000.00"));
        when(transactionRepository.sumAmountByCategory(eq(userId), eq(TransactionType.EXPENSE), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());

        DashboardSummaryDTO summary = dashboardService.getSummary(userId, null, null);

        assertNotNull(summary);
        assertEquals(expectedStart, summary.startDate());
        assertEquals(expectedEnd, summary.endDate());
        assertEquals(new BigDecimal("3000.00"), summary.totalIncome());
        assertEquals(new BigDecimal("1000.00"), summary.totalExpense());
        assertEquals(new BigDecimal("2000.00"), summary.balance());
        assertTrue(summary.expensesByCategory().isEmpty());
    }

    @Test
    @DisplayName("Deve lançar ConflictException quando data inicial for posterior à data final")
    void getSummary_ThrowsConflict_WhenStartDateIsAfterEndDate() {
        LocalDate startDate = LocalDate.of(2026, 11, 20);
        LocalDate endDate = LocalDate.of(2026, 11, 10);

        assertThrows(ConflictException.class, () -> dashboardService.getSummary(userId, startDate, endDate));
    }
}
