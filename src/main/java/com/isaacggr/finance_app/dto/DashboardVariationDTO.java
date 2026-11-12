package com.isaacggr.finance_app.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record DashboardVariationDTO(
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
