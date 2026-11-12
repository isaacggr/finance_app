package com.isaacggr.finance_app.service;

import com.isaacggr.finance_app.domain.entity.Bill;
import com.isaacggr.finance_app.domain.enums.TimeSeriesGranularity;
import com.isaacggr.finance_app.domain.enums.TransactionType;
import com.isaacggr.finance_app.dto.dashboard.CategoryExpenseDTO;
import com.isaacggr.finance_app.dto.dashboard.DashboardSummaryDTO;
import com.isaacggr.finance_app.dto.dashboard.DashboardTimeseriesDTO;
import com.isaacggr.finance_app.exception.BusinessException;
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
    @DisplayName("Deve calcular resumo do dashboard com datas explícitas, período anterior e variações")
    void getSummary_Success_WithExplicitDatesAndVariation() {
        LocalDate startDate = LocalDate.of(2026, 3, 1);
        LocalDate endDate = LocalDate.of(2026, 3, 31);
        LocalDate prevStart = LocalDate.of(2026, 2, 1);
        LocalDate prevEnd = LocalDate.of(2026, 2, 28);

        when(transactionRepository.sumAmountByTypeAndPeriod(userId, TransactionType.INCOME, startDate, endDate))
                .thenReturn(new BigDecimal("5000.00"));
        when(transactionRepository.sumAmountByTypeAndPeriod(userId, TransactionType.EXPENSE, startDate, endDate))
                .thenReturn(new BigDecimal("2100.50"));

        when(transactionRepository.sumAmountByTypeAndPeriod(userId, TransactionType.INCOME, prevStart, prevEnd))
                .thenReturn(new BigDecimal("4000.00"));
        when(transactionRepository.sumAmountByTypeAndPeriod(userId, TransactionType.EXPENSE, prevStart, prevEnd))
                .thenReturn(new BigDecimal("4100.00"));

        List<CategoryExpenseDTO> categories = List.of(
                new CategoryExpenseDTO("ALIMENTAÇÃO", new BigDecimal("1500.00")),
                new CategoryExpenseDTO("TRANSPORTE", new BigDecimal("600.50"))
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

        assertNotNull(summary.previous());
        assertEquals(prevStart, summary.previous().startDate());
        assertEquals(prevEnd, summary.previous().endDate());
        assertEquals(new BigDecimal("-100.00"), summary.previous().balance());

        assertNotNull(summary.variation());
        assertEquals(new BigDecimal("25.00"), summary.variation().incomePercentage());
        assertEquals(new BigDecimal("-48.77"), summary.variation().expensePercentage());
        assertEquals(new BigDecimal("2999.50"), summary.variation().balancePercentage());
        assertEquals(new BigDecimal("1000.00"), summary.variation().incomeDifference());
        assertEquals(new BigDecimal("-1999.50"), summary.variation().expenseDifference());
        assertEquals(new BigDecimal("2999.50"), summary.variation().balanceDifference());
    }

    @Test
    @DisplayName("Deve calcular resumo com datas padrão e retornar variação percentual nula quando base anterior for zero")
    void getSummary_Success_WithDefaultDatesAndZeroPreviousBase() {
        LocalDate today = LocalDate.now(Bill.DEFAULT_ZONE);
        LocalDate expectedStart = today.withDayOfMonth(1);
        LocalDate expectedEnd = today.with(TemporalAdjusters.lastDayOfMonth());

        when(transactionRepository.sumAmountByTypeAndPeriod(eq(userId), eq(TransactionType.INCOME), eq(expectedStart), eq(expectedEnd)))
                .thenReturn(new BigDecimal("3000.00"));
        when(transactionRepository.sumAmountByTypeAndPeriod(eq(userId), eq(TransactionType.EXPENSE), eq(expectedStart), eq(expectedEnd)))
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
        assertNotNull(summary.variation());
        assertNull(summary.variation().incomePercentage());
        assertEquals(new BigDecimal("3000.00"), summary.variation().incomeDifference());
    }

    @Test
    @DisplayName("Deve gerar série temporal diária com zero-filling contínuo e saldo acumulado")
    void getTimeseries_Daily_FillsGapsAndComputesCumulativeBalance() {
        LocalDate start = LocalDate.of(2026, 11, 1);
        LocalDate end = LocalDate.of(2026, 11, 4);

        when(transactionRepository.aggregateByDateAndType(userId, start, end))
                .thenReturn(List.of(
                        new TransactionRepository.DailyAggregate(LocalDate.of(2026, 11, 1), TransactionType.INCOME, new BigDecimal("1000.00")),
                        new TransactionRepository.DailyAggregate(LocalDate.of(2026, 11, 3), TransactionType.EXPENSE, new BigDecimal("250.00"))
                ));

        DashboardTimeseriesDTO series = dashboardService.getTimeseries(userId, start, end, null);

        assertEquals(TimeSeriesGranularity.DAILY, series.granularity());
        assertEquals(4, series.points().size());

        assertEquals("2026-11-01", series.points().get(0).period());
        assertEquals(new BigDecimal("1000.00"), series.points().get(0).balance());
        assertEquals(new BigDecimal("1000.00"), series.points().get(0).cumulativeBalance());

        assertEquals("2026-11-02", series.points().get(1).period());
        assertEquals(new BigDecimal("0.00"), series.points().get(1).income());
        assertEquals(new BigDecimal("0.00"), series.points().get(1).expense());
        assertEquals(new BigDecimal("1000.00"), series.points().get(1).cumulativeBalance());

        assertEquals("2026-11-03", series.points().get(2).period());
        assertEquals(new BigDecimal("-250.00"), series.points().get(2).balance());
        assertEquals(new BigDecimal("750.00"), series.points().get(2).cumulativeBalance());
    }

    @Test
    @DisplayName("Deve escolher granularidade MONTHLY automaticamente para períodos longos e validar limites")
    void getTimeseries_MonthlyAutoAndLimits() {
        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = LocalDate.of(2026, 11, 30);

        when(transactionRepository.aggregateByDateAndType(userId, start, end))
                .thenReturn(List.of(
                        new TransactionRepository.DailyAggregate(LocalDate.of(2026, 9, 5), TransactionType.INCOME, new BigDecimal("3000.00")),
                        new TransactionRepository.DailyAggregate(LocalDate.of(2026, 9, 10), TransactionType.EXPENSE, new BigDecimal("1200.00")),
                        new TransactionRepository.DailyAggregate(LocalDate.of(2026, 11, 1), TransactionType.EXPENSE, new BigDecimal("500.00"))
                ));

        DashboardTimeseriesDTO series = dashboardService.getTimeseries(userId, start, end, null);

        assertEquals(TimeSeriesGranularity.MONTHLY, series.granularity());
        assertEquals(3, series.points().size());
        assertEquals("2026-09", series.points().get(0).period());
        assertEquals(new BigDecimal("1800.00"), series.points().get(0).balance());
        assertEquals("2026-10", series.points().get(1).period());
        assertEquals(new BigDecimal("1800.00"), series.points().get(1).cumulativeBalance());
        assertEquals("2026-11", series.points().get(2).period());
        assertEquals(new BigDecimal("1300.00"), series.points().get(2).cumulativeBalance());

        assertThrows(BusinessException.class, () -> dashboardService.getTimeseries(
                userId, LocalDate.of(2025, 1, 1), LocalDate.of(2026, 12, 31), TimeSeriesGranularity.DAILY));
    }

    @Test
    @DisplayName("Deve lançar ConflictException quando data inicial for posterior à data final")
    void getSummary_ThrowsConflict_WhenStartDateIsAfterEndDate() {
        LocalDate startDate = LocalDate.of(2026, 11, 20);
        LocalDate endDate = LocalDate.of(2026, 11, 10);

        assertThrows(ConflictException.class, () -> dashboardService.getSummary(userId, startDate, endDate));
    }
}
