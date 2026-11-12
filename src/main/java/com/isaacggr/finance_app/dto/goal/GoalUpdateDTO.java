package com.isaacggr.finance_app.dto.goal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GoalUpdateDTO(
        @Schema(description = "Título atualizado da meta financeira", example = "Reserva de Emergência Ampliada")
        @NotBlank(message = "O título da meta é obrigatório")
        @Size(min = 2, max = 80, message = "O título deve ter entre 2 e 80 caracteres")
        String title,

        @Schema(description = "Descrição opcional da meta (envie null ou vazio para limpar)", example = "12 meses de despesas fixas")
        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres")
        String description,

        @Schema(description = "Novo valor alvo da meta (escala 2, RoundingMode.HALF_EVEN)", example = "20000.00")
        @NotNull(message = "O valor alvo da meta é obrigatório")
        @Positive(message = "O valor alvo deve ser positivo")
        @DecimalMin(value = "0.01", message = "O valor mínimo é 0.01")
        @DecimalMax(value = "9999999999999.99", message = "O valor máximo permitido é 9999999999999.99")
        @Digits(integer = 13, fraction = 2, message = "O valor deve ter no máximo 13 dígitos inteiros e 2 casas decimais")
        BigDecimal targetAmount,

        @Schema(description = "Nova data alvo opcional (AAAA-MM-DD)", example = "2028-06-30")
        LocalDate deadline,

        @Schema(description = "Define ou remove o destaque principal (opcional; mantém o estado atual se omitido)", example = "true")
        Boolean primary
) {
}
