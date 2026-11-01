package com.isaacggr.finance_app.dto;

import com.isaacggr.finance_app.domain.enums.TransactionCategory;
import com.isaacggr.finance_app.domain.enums.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionUpdateDTO(

        @Schema(description = "Novo valor da transação", example = "150.00")
        @NotNull(message = "O valor da transação é obrigatório")
        @Positive(message = "O valor da transação deve ser positivo")
        @DecimalMin(value = "0.01", message = "O valor da transação deve ser de no mínimo 0.01")
        BigDecimal amount,

        @Schema(description = "Novo tipo da transação (INCOME ou EXPENSE)", example = "EXPENSE")
        @NotNull(message = "O tipo da transação é obrigatório")
        TransactionType type,

        @Schema(description = "Nova categoria da transação", example = "ALIMENTACAO")
        @NotNull(message = "A categoria da transação é obrigatória")
        TransactionCategory category,

        @Schema(description = "Nova data da transação (formato AAAA-MM-DD)", example = "2026-11-01")
        @NotNull(message = "A data da transação é obrigatória")
        LocalDate date,

        @Schema(description = "Nova descrição detalhada", example = "Almoço em restaurante")
        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres")
        String description
) {
}

