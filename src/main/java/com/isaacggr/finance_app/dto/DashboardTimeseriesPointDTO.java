package com.isaacggr.finance_app.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DashboardTimeseriesPointDTO(
        @Schema(description = "Identificador textual do ponto (AAAA-MM-DD para DAILY ou AAAA-MM para MONTHLY)", example = "2026-11-01")
        String period,

        @Schema(description = "Data de referência do ponto (primeiro dia do mês quando MONTHLY)", example = "2026-11-01")
        LocalDate date,

        @Schema(description = "Receitas do bucket", example = "2500.00")
        BigDecimal income,

        @Schema(description = "Despesas do bucket", example = "420.50")
        BigDecimal expense,

        @Schema(description = "Saldo líquido do bucket (receitas - despesas)", example = "2079.50")
        BigDecimal balance,

        @Schema(description = "Saldo acumulado dentro do intervalo selecionado até este ponto", example = "2079.50")
        BigDecimal cumulativeBalance
) {
}
