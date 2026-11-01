package com.isaacggr.finance_app.service;

import com.isaacggr.finance_app.domain.enums.TransactionType;
import com.isaacggr.finance_app.dto.CategoryExpenseDTO;
import com.isaacggr.finance_app.dto.DashboardSummaryDTO;
import com.isaacggr.finance_app.exception.ConflictException;
import com.isaacggr.finance_app.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final TransactionRepository transactionRepository;

    public DashboardService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public DashboardSummaryDTO getSummary(UUID userId, LocalDate startDate, LocalDate endDate) {
        // Se as datas não forem informadas, assume o mês corrente (do primeiro ao último dia)
        LocalDate effectiveStartDate = (startDate != null)
                ? startDate
                : LocalDate.now().withDayOfMonth(1);

        LocalDate effectiveEndDate = (endDate != null)
                ? endDate
                : LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

        if (effectiveStartDate.isAfter(effectiveEndDate)) {
            throw new ConflictException("A data inicial não pode ser posterior à data final");
        }

        // Consultas com agregação SQL direta no MySQL (baixo uso de memória e alta velocidade)
        BigDecimal totalIncome = transactionRepository
                .sumAmountByTypeAndPeriod(userId, TransactionType.INCOME, effectiveStartDate, effectiveEndDate);

        BigDecimal totalExpense = transactionRepository
                .sumAmountByTypeAndPeriod(userId, TransactionType.EXPENSE, effectiveStartDate, effectiveEndDate);

        BigDecimal balance = totalIncome.subtract(totalExpense);

        List<CategoryExpenseDTO> expensesByCategory = transactionRepository
                .sumAmountByCategory(userId, TransactionType.EXPENSE, effectiveStartDate, effectiveEndDate);

        return new DashboardSummaryDTO(
                effectiveStartDate,
                effectiveEndDate,
                totalIncome,
                totalExpense,
                balance,
                expensesByCategory
        );
    }
}
