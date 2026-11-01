package com.isaacggr.finance_app.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardSummaryDTO(
        @Schema(description = "Data inicial do período analisado", example = "2026-11-01")
        LocalDate startDate,

        @Schema(description = "Data final do período analisado", example = "2026-11-30")
        LocalDate endDate,

        @Schema(description = "Soma total de receitas do período", example = "5000.00")
        BigDecimal totalIncome,

        @Schema(description = "Soma total de despesas do período", example = "2150.50")
        BigDecimal totalExpense,

        @Schema(description = "Saldo líquido do período (receitas - despesas)", example = "2849.50")
        BigDecimal balance,

        @Schema(description = "Detalhamento de gastos agrupados por categoria")
        List<CategoryExpenseDTO> expensesByCategory
) {
}
