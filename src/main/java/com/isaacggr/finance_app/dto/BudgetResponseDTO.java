package com.isaacggr.finance_app.dto;

import com.isaacggr.finance_app.domain.entity.Budget;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

public record BudgetResponseDTO(
        @Schema(description = "Identificador único do orçamento", example = "6fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Categoria da despesa", example = "ALIMENTACAO")
        String category,

        @Schema(description = "Valor teto planejado", example = "800.00")
        BigDecimal amount,

        @Schema(description = "Mês de referência", example = "11")
        int month,

        @Schema(description = "Ano de referência", example = "2026")
        int year
) {
    public static BudgetResponseDTO fromEntity(Budget budget) {
        return new BudgetResponseDTO(
                budget.getId(),
                budget.getCategory(),
                budget.getAmount(),
                budget.getMonth(),
                budget.getYear()
        );
    }
}
