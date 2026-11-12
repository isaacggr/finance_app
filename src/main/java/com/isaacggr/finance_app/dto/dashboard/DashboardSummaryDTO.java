package com.isaacggr.finance_app.dto.dashboard;

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
        List<CategoryExpenseDTO> expensesByCategory,

        @Schema(description = "Totais consolidados do período anterior equivalente para comparação")
        PreviousPeriod previous,

        @Schema(description = "Variações percentuais e absolutas em relação ao período anterior")
        Variation variation
) {
    public DashboardSummaryDTO(
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal totalIncome,
            BigDecimal totalExpense,
            BigDecimal balance,
            List<CategoryExpenseDTO> expensesByCategory
    ) {
        this(startDate, endDate, totalIncome, totalExpense, balance, expensesByCategory, null, null);
    }

    @Schema(name = "DashboardPreviousPeriod", description = "Totais consolidados do período anterior equivalente")
    public record PreviousPeriod(
            @Schema(description = "Data inicial do período anterior comparado", example = "2026-10-01")
            LocalDate startDate,

            @Schema(description = "Data final do período anterior comparado", example = "2026-10-31")
            LocalDate endDate,

            @Schema(description = "Soma total de receitas do período anterior", example = "4500.00")
            BigDecimal totalIncome,

            @Schema(description = "Soma total de despesas do período anterior", example = "2000.00")
            BigDecimal totalExpense,

            @Schema(description = "Saldo líquido do período anterior", example = "2500.00")
            BigDecimal balance
    ) {
    }

    @Schema(name = "DashboardVariation", description = "Variações percentuais e absolutas em relação ao período anterior")
    public record Variation(
            @Schema(description = "Variação percentual de receitas vs. período anterior (null quando base anterior for 0 e atual diferente de 0)", example = "11.11")
            BigDecimal incomePercentage,

            @Schema(description = "Variação percentual de despesas vs. período anterior (null quando base anterior for 0 e atual diferente de 0)", example = "7.53")
            BigDecimal expensePercentage,

            @Schema(description = "Variação percentual do saldo líquido vs. período anterior (null quando base anterior for 0 e atual diferente de 0)", example = "13.98")
            BigDecimal balancePercentage,

            @Schema(description = "Diferença absoluta em reais nas receitas (atual - anterior)", example = "500.00")
            BigDecimal incomeDifference,

            @Schema(description = "Diferença absoluta em reais nas despesas (atual - anterior)", example = "150.50")
            BigDecimal expenseDifference,

            @Schema(description = "Diferença absoluta em reais no saldo líquido (atual - anterior)", example = "349.50")
            BigDecimal balanceDifference
    ) {
    }
}
