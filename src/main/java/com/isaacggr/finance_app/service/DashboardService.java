package com.isaacggr.finance_app.service;

import com.isaacggr.finance_app.domain.entity.Bill;
import com.isaacggr.finance_app.domain.entity.Transaction;
import com.isaacggr.finance_app.domain.enums.TimeSeriesGranularity;
import com.isaacggr.finance_app.domain.enums.TransactionType;
import com.isaacggr.finance_app.dto.dashboard.CategoryExpenseDTO;
import com.isaacggr.finance_app.dto.dashboard.DashboardSummaryDTO;
import com.isaacggr.finance_app.dto.dashboard.DashboardTimeseriesDTO;
import com.isaacggr.finance_app.exception.BusinessException;
import com.isaacggr.finance_app.exception.ConflictException;
import com.isaacggr.finance_app.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final int AUTO_DAILY_MAX_DAYS = 45;
    private static final int MAX_DAILY_RANGE_DAYS = 366;
    private static final int MAX_MONTHLY_RANGE_MONTHS = 60;
    private static final int DEFAULT_MONTHLY_LOOKBACK_MONTHS = 6;

    private final TransactionRepository transactionRepository;

    public DashboardService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public DashboardSummaryDTO getSummary(UUID userId, LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now(Bill.DEFAULT_ZONE);

        // Se as datas não forem informadas, assume o mês corrente (do primeiro ao último dia em America/Sao_Paulo)
        LocalDate effectiveStartDate = (startDate != null)
                ? startDate
                : today.withDayOfMonth(1);

        LocalDate effectiveEndDate = (endDate != null)
                ? endDate
                : today.with(TemporalAdjusters.lastDayOfMonth());

        if (effectiveStartDate.isAfter(effectiveEndDate)) {
            throw new ConflictException("A data inicial não pode ser posterior à data final");
        }

        // Consultas com agregação SQL direta no MySQL (baixo uso de memória e alta velocidade)
        BigDecimal totalIncome = coalesceZero(transactionRepository
                .sumAmountByTypeAndPeriod(userId, TransactionType.INCOME, effectiveStartDate, effectiveEndDate));

        BigDecimal totalExpense = coalesceZero(transactionRepository
                .sumAmountByTypeAndPeriod(userId, TransactionType.EXPENSE, effectiveStartDate, effectiveEndDate));

        BigDecimal balance = normalizeScale(totalIncome.subtract(totalExpense));

        List<CategoryExpenseDTO> expensesByCategory = transactionRepository
                .sumAmountByCategory(userId, TransactionType.EXPENSE, effectiveStartDate, effectiveEndDate);

        LocalDate[] previousRange = resolvePreviousPeriod(effectiveStartDate, effectiveEndDate);
        LocalDate prevStart = previousRange[0];
        LocalDate prevEnd = previousRange[1];

        BigDecimal previousIncome = coalesceZero(transactionRepository
                .sumAmountByTypeAndPeriod(userId, TransactionType.INCOME, prevStart, prevEnd));

        BigDecimal previousExpense = coalesceZero(transactionRepository
                .sumAmountByTypeAndPeriod(userId, TransactionType.EXPENSE, prevStart, prevEnd));

        BigDecimal previousBalance = normalizeScale(previousIncome.subtract(previousExpense));

        DashboardSummaryDTO.PreviousPeriod previous = new DashboardSummaryDTO.PreviousPeriod(
                prevStart,
                prevEnd,
                previousIncome,
                previousExpense,
                previousBalance
        );

        DashboardSummaryDTO.Variation variation = new DashboardSummaryDTO.Variation(
                calculatePercentageVariation(totalIncome, previousIncome),
                calculatePercentageVariation(totalExpense, previousExpense),
                calculatePercentageVariation(balance, previousBalance),
                normalizeScale(totalIncome.subtract(previousIncome)),
                normalizeScale(totalExpense.subtract(previousExpense)),
                normalizeScale(balance.subtract(previousBalance))
        );

        return new DashboardSummaryDTO(
                effectiveStartDate,
                effectiveEndDate,
                totalIncome,
                totalExpense,
                balance,
                expensesByCategory,
                previous,
                variation
        );
    }

    public DashboardTimeseriesDTO getTimeseries(
            UUID userId,
            LocalDate startDate,
            LocalDate endDate,
            TimeSeriesGranularity granularity
    ) {
        LocalDate today = LocalDate.now(Bill.DEFAULT_ZONE);
        LocalDate effectiveStartDate;
        LocalDate effectiveEndDate;

        if (startDate == null && endDate == null && granularity == TimeSeriesGranularity.MONTHLY) {
            YearMonth currentMonth = YearMonth.from(today);
            effectiveStartDate = currentMonth.minusMonths(DEFAULT_MONTHLY_LOOKBACK_MONTHS - 1).atDay(1);
            effectiveEndDate = currentMonth.atEndOfMonth();
        } else {
            effectiveStartDate = (startDate != null) ? startDate : today.withDayOfMonth(1);
            effectiveEndDate = (endDate != null) ? endDate : today.with(TemporalAdjusters.lastDayOfMonth());
        }

        if (effectiveStartDate.isAfter(effectiveEndDate)) {
            throw new ConflictException("A data inicial não pode ser posterior à data final");
        }

        long spanDays = ChronoUnit.DAYS.between(effectiveStartDate, effectiveEndDate) + 1;
        TimeSeriesGranularity effectiveGranularity = (granularity != null)
                ? granularity
                : (spanDays <= AUTO_DAILY_MAX_DAYS ? TimeSeriesGranularity.DAILY : TimeSeriesGranularity.MONTHLY);

        validateRangeLimit(effectiveStartDate, effectiveEndDate, effectiveGranularity, spanDays);

        List<TransactionRepository.DailyAggregate> rows = transactionRepository.aggregateByDateAndType(
                userId, effectiveStartDate, effectiveEndDate
        );

        List<DashboardTimeseriesDTO.Point> points = (effectiveGranularity == TimeSeriesGranularity.DAILY)
                ? buildDailyPoints(effectiveStartDate, effectiveEndDate, rows)
                : buildMonthlyPoints(effectiveStartDate, effectiveEndDate, rows);

        return new DashboardTimeseriesDTO(effectiveStartDate, effectiveEndDate, effectiveGranularity, points);
    }

    private void validateRangeLimit(
            LocalDate startDate,
            LocalDate endDate,
            TimeSeriesGranularity granularity,
            long spanDays
    ) {
        if (granularity == TimeSeriesGranularity.DAILY && spanDays > MAX_DAILY_RANGE_DAYS) {
            throw new BusinessException(
                    "Para granularidade diária, o período máximo é de " + MAX_DAILY_RANGE_DAYS
                            + " dias. Utilize granularity=MONTHLY para intervalos maiores."
            );
        }

        if (granularity == TimeSeriesGranularity.MONTHLY) {
            long monthSpan = ChronoUnit.MONTHS.between(YearMonth.from(startDate), YearMonth.from(endDate)) + 1;
            if (monthSpan > MAX_MONTHLY_RANGE_MONTHS) {
                throw new BusinessException(
                        "Para granularidade mensal, o período máximo é de " + MAX_MONTHLY_RANGE_MONTHS + " meses."
                );
            }
        }
    }

    private List<DashboardTimeseriesDTO.Point> buildDailyPoints(
            LocalDate startDate,
            LocalDate endDate,
            List<TransactionRepository.DailyAggregate> rows
    ) {
        Map<LocalDate, BigDecimal> incomeMap = new HashMap<>();
        Map<LocalDate, BigDecimal> expenseMap = new HashMap<>();

        if (rows != null) {
            for (TransactionRepository.DailyAggregate row : rows) {
                if (row.type() == TransactionType.INCOME) {
                    incomeMap.merge(row.date(), coalesceZero(row.total()), BigDecimal::add);
                } else if (row.type() == TransactionType.EXPENSE) {
                    expenseMap.merge(row.date(), coalesceZero(row.total()), BigDecimal::add);
                }
            }
        }

        List<DashboardTimeseriesDTO.Point> points = new ArrayList<>();
        BigDecimal cumulative = normalizeScale(BigDecimal.ZERO);

        for (LocalDate cursor = startDate; !cursor.isAfter(endDate); cursor = cursor.plusDays(1)) {
            BigDecimal income = normalizeScale(incomeMap.getOrDefault(cursor, BigDecimal.ZERO));
            BigDecimal expense = normalizeScale(expenseMap.getOrDefault(cursor, BigDecimal.ZERO));
            BigDecimal balance = normalizeScale(income.subtract(expense));
            cumulative = normalizeScale(cumulative.add(balance));

            points.add(new DashboardTimeseriesDTO.Point(
                    cursor.toString(),
                    cursor,
                    income,
                    expense,
                    balance,
                    cumulative
            ));
        }

        return points;
    }

    private List<DashboardTimeseriesDTO.Point> buildMonthlyPoints(
            LocalDate startDate,
            LocalDate endDate,
            List<TransactionRepository.DailyAggregate> rows
    ) {
        Map<YearMonth, BigDecimal> incomeMap = new HashMap<>();
        Map<YearMonth, BigDecimal> expenseMap = new HashMap<>();

        if (rows != null) {
            for (TransactionRepository.DailyAggregate row : rows) {
                YearMonth ym = YearMonth.from(row.date());
                if (row.type() == TransactionType.INCOME) {
                    incomeMap.merge(ym, coalesceZero(row.total()), BigDecimal::add);
                } else if (row.type() == TransactionType.EXPENSE) {
                    expenseMap.merge(ym, coalesceZero(row.total()), BigDecimal::add);
                }
            }
        }

        List<DashboardTimeseriesDTO.Point> points = new ArrayList<>();
        BigDecimal cumulative = normalizeScale(BigDecimal.ZERO);

        YearMonth startYm = YearMonth.from(startDate);
        YearMonth endYm = YearMonth.from(endDate);

        for (YearMonth cursor = startYm; !cursor.isAfter(endYm); cursor = cursor.plusMonths(1)) {
            BigDecimal income = normalizeScale(incomeMap.getOrDefault(cursor, BigDecimal.ZERO));
            BigDecimal expense = normalizeScale(expenseMap.getOrDefault(cursor, BigDecimal.ZERO));
            BigDecimal balance = normalizeScale(income.subtract(expense));
            cumulative = normalizeScale(cumulative.add(balance));

            points.add(new DashboardTimeseriesDTO.Point(
                    cursor.toString(),
                    cursor.atDay(1),
                    income,
                    expense,
                    balance,
                    cumulative
            ));
        }

        return points;
    }

    private LocalDate[] resolvePreviousPeriod(LocalDate startDate, LocalDate endDate) {
        YearMonth startYm = YearMonth.from(startDate);
        YearMonth endYm = YearMonth.from(endDate);

        // Compara meses calendários completos entre si (evita truncar dias em meses de 28/30/31 dias)
        boolean isFullMonthAligned = startDate.getDayOfMonth() == 1
                && endDate.equals(endYm.atEndOfMonth())
                && !endYm.isBefore(startYm);

        if (isFullMonthAligned) {
            long monthSpan = ChronoUnit.MONTHS.between(startYm, endYm) + 1;
            YearMonth prevStartYm = startYm.minusMonths(monthSpan);
            YearMonth prevEndYm = startYm.minusMonths(1);
            return new LocalDate[]{prevStartYm.atDay(1), prevEndYm.atEndOfMonth()};
        }

        long daySpan = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        LocalDate prevEnd = startDate.minusDays(1);
        LocalDate prevStart = prevEnd.minusDays(daySpan - 1);
        return new LocalDate[]{prevStart, prevEnd};
    }

    private BigDecimal calculatePercentageVariation(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return (current.compareTo(BigDecimal.ZERO) == 0)
                    ? normalizeScale(BigDecimal.ZERO)
                    : null;
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous.abs(), 2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal coalesceZero(BigDecimal value) {
        return (value != null)
                ? normalizeScale(value)
                : normalizeScale(BigDecimal.ZERO);
    }

    private BigDecimal normalizeScale(BigDecimal value) {
        return Transaction.normalizeMonetaryAmount(value);
    }
}
