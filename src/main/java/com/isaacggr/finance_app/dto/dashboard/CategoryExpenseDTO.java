package com.isaacggr.finance_app.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record CategoryExpenseDTO(
        @Schema(description = "Categoria da despesa", example = "ALIMENTACAO")
        String category,

        @Schema(description = "Total gasto na categoria", example = "450.00")
        BigDecimal total
) {
}
