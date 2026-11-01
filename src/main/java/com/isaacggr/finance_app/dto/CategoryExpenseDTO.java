package com.isaacggr.finance_app.dto;

import com.isaacggr.finance_app.domain.enums.TransactionCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record CategoryExpenseDTO(
        @Schema(description = "Categoria da despesa", example = "ALIMENTACAO")
        TransactionCategory category,

        @Schema(description = "Total acumulado de despesas na categoria", example = "450.75")
        BigDecimal total
) {
}
