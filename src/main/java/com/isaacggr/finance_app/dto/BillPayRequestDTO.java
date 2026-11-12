package com.isaacggr.finance_app.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

public record BillPayRequestDTO(
        @Schema(description = "Define se o pagamento deve gerar automaticamente uma despesa em /transactions (padrão true)", example = "true")
        Boolean createTransaction,

        @Schema(description = "Data efetiva do pagamento (AAAA-MM-DD); assume hoje em America/Sao_Paulo quando omitida", example = "2026-11-11")
        LocalDate paidDate
) {
}
