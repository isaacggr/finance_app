package com.isaacggr.finance_app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GoalCreateDTO(
        @Schema(description = "Título da meta financeira", example = "Reserva de Emergência")
        @NotBlank(message = "O título da meta é obrigatório")
        @Size(min = 2, max = 80, message = "O título deve ter entre 2 e 80 caracteres")
        String title,

        @Schema(description = "Descrição opcional com detalhes do objetivo", example = "Guardar 6 meses de despesas fixas")
        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres")
        String description,

        @Schema(description = "Valor total alvo da meta (escala 2, RoundingMode.HALF_EVEN)", example = "15000.00")
        @NotNull(message = "O valor alvo da meta é obrigatório")
        @Positive(message = "O valor alvo deve ser positivo")
        @DecimalMin(value = "0.01", message = "O valor mínimo é 0.01")
        @DecimalMax(value = "9999999999999.99", message = "O valor máximo permitido é 9999999999999.99")
        @Digits(integer = 13, fraction = 2, message = "O valor deve ter no máximo 13 dígitos inteiros e 2 casas decimais")
        BigDecimal targetAmount,

        @Schema(description = "Saldo inicial opcional; quando maior que zero, cria o primeiro aporte automaticamente", example = "1000.00")
        @DecimalMin(value = "0.00", message = "O valor inicial não pode ser negativo")
        @DecimalMax(value = "9999999999999.99", message = "O valor máximo permitido é 9999999999999.99")
        @Digits(integer = 13, fraction = 2, message = "O valor deve ter no máximo 13 dígitos inteiros e 2 casas decimais")
        BigDecimal initialAmount,

        @Schema(description = "Data alvo opcional para concluir a meta (AAAA-MM-DD)", example = "2027-12-31")
        LocalDate deadline,

        @Schema(description = "Define se esta passa a ser a meta principal do usuário (padrão true quando omitido)", example = "true")
        Boolean primary
) {
}
