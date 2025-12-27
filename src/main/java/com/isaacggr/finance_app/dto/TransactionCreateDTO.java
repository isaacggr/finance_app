package com.isaacggr.finance_app.dto;

import com.isaacggr.finance_app.domain.enums.TransactionCategory;
import com.isaacggr.finance_app.domain.enums.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionCreateDTO(

                @NotNull UUID userId,

                @NotNull BigDecimal amount,

                @NotNull TransactionType type,

                @NotNull TransactionCategory category,

                @NotNull LocalDate date,

                @Size(max = 255) String description) {
}
