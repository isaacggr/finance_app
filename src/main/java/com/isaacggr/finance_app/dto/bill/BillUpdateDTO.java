package com.isaacggr.finance_app.dto.bill;

import com.isaacggr.finance_app.domain.enums.BillRecurrence;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BillUpdateDTO(
        @Schema(description = "Título atualizado da conta a pagar", example = "Internet Fibra 500MB")
        @NotBlank(message = "O título da conta é obrigatório")
        @Size(min = 2, max = 80, message = "O título deve ter entre 2 e 80 caracteres")
        String title,

        @Schema(description = "Observação opcional (envie null ou vazio para limpar)", example = "Plano reajustado em novembro")
        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres")
        String description,

        @Schema(description = "Categoria livre atualizada", example = "MORADIA")
        @NotBlank(message = "A categoria da conta é obrigatória")
        @Size(max = 30, message = "A categoria deve ter no máximo 30 caracteres")
        String category,

        @Schema(description = "Valor atualizado da conta (escala 2, RoundingMode.HALF_EVEN)", example = "149.90")
        @NotNull(message = "O valor da conta é obrigatório")
        @Positive(message = "O valor da conta deve ser positivo")
        @DecimalMin(value = "0.01", message = "O valor mínimo é 0.01")
        @DecimalMax(value = "9999999999999.99", message = "O valor máximo permitido é 9999999999999.99")
        @Digits(integer = 13, fraction = 2, message = "O valor deve ter no máximo 13 dígitos inteiros e 2 casas decimais")
        BigDecimal amount,

        @Schema(description = "Nova data de vencimento (AAAA-MM-DD)", example = "2026-11-20")
        @NotNull(message = "A data de vencimento é obrigatória")
        LocalDate dueDate,

        @Schema(description = "Recorrência atualizada (ONCE, MONTHLY ou YEARLY; padrão ONCE)", example = "MONTHLY")
        BillRecurrence recurrence
) {
}
