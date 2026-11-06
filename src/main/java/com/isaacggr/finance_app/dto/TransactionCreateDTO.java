package com.isaacggr.finance_app.dto;

import com.isaacggr.finance_app.domain.enums.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionCreateDTO(

        @Schema(description = "Valor da transação", example = "89.90")
        @NotNull(message = "O valor da transação é obrigatório")
        @Positive(message = "O valor da transação deve ser positivo")
        @DecimalMin(value = "0.01", message = "O valor da transação deve ser de no mínimo 0.01")
        BigDecimal amount,

        @Schema(description = "Tipo da transação (INCOME ou EXPENSE)", example = "EXPENSE")
        @NotNull(message = "O tipo da transação é obrigatório")
        TransactionType type,

        @Schema(description = "Categoria da transação (livre, ex: ALIMENTACAO, PETS, FARMACIA)", example = "ALIMENTACAO")
        @NotBlank(message = "A categoria da transação é obrigatória")
        @Size(max = 30, message = "A categoria deve ter no máximo 30 caracteres")
        String category,

        @Schema(description = "Data da transação (formato AAAA-MM-DD)", example = "2026-11-01")
        @NotNull(message = "A data da transação é obrigatória")
        LocalDate date,

        @Schema(description = "Descrição detalhada opcional", example = "Supermercado do mês")
        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres")
        String description
) {
}
