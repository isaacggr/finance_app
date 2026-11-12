package com.isaacggr.finance_app.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BillPayResponseDTO(
        @Schema(description = "Conta marcada como paga")
        BillResponseDTO bill,

        @Schema(description = "ID da despesa lançada automaticamente em /transactions (omitido se createTransaction=false)", example = "c31f7f64-2b43-4c11-941a-758420c45999")
        UUID generatedTransactionId,

        @Schema(description = "Próxima conta pendente criada automaticamente quando a recorrência é MONTHLY ou YEARLY")
        BillResponseDTO nextBill
) {
}
