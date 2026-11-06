package com.isaacggr.finance_app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record BudgetCreateDTO(
        @Schema(description = "Categoria da despesa para definir o limite (ex: ALIMENTACAO, PETS)", example = "ALIMENTACAO")
        @NotBlank(message = "A categoria é obrigatória")
        @Size(max = 30, message = "A categoria deve ter no máximo 30 caracteres")
        String category,

        @Schema(description = "Valor teto máximo planejado para a categoria no mês", example = "800.00")
        @NotNull(message = "O valor do orçamento é obrigatório")
        @Positive(message = "O valor deve ser positivo")
        @DecimalMin(value = "0.01", message = "O valor mínimo é 0.01")
        BigDecimal amount,

        @Schema(description = "Mês de referência (1 a 12)", example = "11")
        @NotNull(message = "O mês é obrigatório")
        @Min(value = 1, message = "O mês deve ser entre 1 e 12")
        @Max(value = 12, message = "O mês deve ser entre 1 e 12")
        Integer month,

        @Schema(description = "Ano de referência", example = "2026")
        @NotNull(message = "O ano é obrigatório")
        @Min(value = 2000, message = "O ano deve ser no mínimo 2000")
        Integer year
) {
}
