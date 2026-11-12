package com.isaacggr.finance_app.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DashboardPreviousPeriodDTO(
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
