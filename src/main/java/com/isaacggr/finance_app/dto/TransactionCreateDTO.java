package com.isaacggr.finance_app.dto;

import com.isaacggr.finance_app.domain.enums.TransactionCategory;
import com.isaacggr.finance_app.domain.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionCreateDTO(

        @NotNull(message = "O ID do usuário é obrigatório")
        UUID userId,

        @NotNull(message = "O valor da transação é obrigatório")
        @Positive(message = "O valor da transação deve ser positivo")
        @DecimalMin(value = "0.01", message = "O valor da transação deve ser de no mínimo 0.01")
        BigDecimal amount,

        @NotNull(message = "O tipo da transação é obrigatório")
        TransactionType type,

        @NotNull(message = "A categoria da transação é obrigatória")
        TransactionCategory category,

        @NotNull(message = "A data da transação é obrigatória")
        LocalDate date,

        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres")
        String description
) {
}
