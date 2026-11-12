package com.isaacggr.finance_app.dto.budget;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record BudgetUpdateDTO(
        @Schema(description = "Nova categoria da despesa (opcional; mantém a atual se omitida)", example = "ALIMENTACAO")
        @Size(max = 30, message = "A categoria deve ter no máximo 30 caracteres")
        @Pattern(regexp = ".*\\S.*", message = "A categoria não pode ficar em branco")
        String category,

        @Schema(description = "Novo valor teto máximo planejado para a categoria no mês (escala 2, RoundingMode.HALF_EVEN)", example = "950.00")
        @NotNull(message = "O valor do orçamento é obrigatório")
        @Positive(message = "O valor deve ser positivo")
        @DecimalMin(value = "0.01", message = "O valor mínimo é 0.01")
        @DecimalMax(value = "9999999999999.99", message = "O valor deve ser no máximo 9999999999999.99")
        @Digits(integer = 13, fraction = 2, message = "O valor deve ter no máximo 13 dígitos inteiros e 2 casas decimais")
        BigDecimal amount,

        @Schema(description = "Novo mês de referência (1 a 12, opcional; mantém o atual se omitido)", example = "11")
        @Min(value = 1, message = "O mês deve ser entre 1 e 12")
        @Max(value = 12, message = "O mês deve ser entre 1 e 12")
        Integer month,

        @Schema(description = "Novo ano de referência (opcional; mantém o atual se omitido)", example = "2026")
        @Min(value = 2000, message = "O ano deve ser no mínimo 2000")
        Integer year
) {
}
