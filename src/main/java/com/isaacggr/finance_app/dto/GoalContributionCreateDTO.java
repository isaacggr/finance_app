package com.isaacggr.finance_app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GoalContributionCreateDTO(
        @Schema(description = "Valor do aporte realizado na meta (escala 2, RoundingMode.HALF_EVEN)", example = "500.00")
        @NotNull(message = "O valor do aporte é obrigatório")
        @Positive(message = "O valor do aporte deve ser positivo")
        @DecimalMin(value = "0.01", message = "O valor mínimo do aporte é 0.01")
        @DecimalMax(value = "9999999999999.99", message = "O valor máximo permitido é 9999999999999.99")
        @Digits(integer = 13, fraction = 2, message = "O valor deve ter no máximo 13 dígitos inteiros e 2 casas decimais")
        BigDecimal amount,

        @Schema(description = "Data do aporte (AAAA-MM-DD); assume a data atual em America/Sao_Paulo quando omitida", example = "2026-11-11")
        LocalDate contributionDate,

        @Schema(description = "Observação opcional sobre o aporte", example = "13º salário - 1ª parcela")
        @Size(max = 255, message = "A observação deve ter no máximo 255 caracteres")
        String note
) {
}
