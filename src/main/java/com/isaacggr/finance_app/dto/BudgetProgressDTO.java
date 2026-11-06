package com.isaacggr.finance_app.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

public record BudgetProgressDTO(
        @Schema(description = "Identificador do orçamento", example = "6fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Categoria da meta", example = "ALIMENTACAO")
        String category,

        @Schema(description = "Valor teto planejado (limite)", example = "800.00")
        BigDecimal budgetAmount,

        @Schema(description = "Valor real já gasto em transações no mês", example = "640.00")
        BigDecimal spentAmount,

        @Schema(description = "Valor restante disponível antes de estourar a meta", example = "160.00")
        BigDecimal remainingAmount,

        @Schema(description = "Percentual consumido da meta (0% a 100%+)", example = "80.0")
        BigDecimal percentageUsed,

        @Schema(description = "Indica se o orçamento foi estourado (gasto > teto)", example = "false")
        boolean exceeded
) {
}
