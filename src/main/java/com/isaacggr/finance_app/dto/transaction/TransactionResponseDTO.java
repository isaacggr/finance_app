package com.isaacggr.finance_app.dto.transaction;

import com.isaacggr.finance_app.domain.entity.Transaction;
import com.isaacggr.finance_app.domain.enums.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Dados de retorno de uma transação financeira")
public record TransactionResponseDTO(
        @Schema(description = "Identificador único da transação", example = "a6703554-1fe5-412f-9b09-cf80ef2df81c")
        UUID id,

        @Schema(description = "Valor monetário da transação (escala 2, RoundingMode.HALF_EVEN)", example = "120.00")
        BigDecimal amount,

        @Schema(description = "Tipo da transação (INCOME ou EXPENSE)", example = "EXPENSE")
        TransactionType type,

        @Schema(description = "Categoria da transação", example = "ELETRONICOS")
        String category,

        @Schema(description = "Data da transação (formato AAAA-MM-DD)", example = "2026-11-15")
        LocalDate date,

        @Schema(description = "Descrição detalhada da transação", example = "Notebook Dell Inspiron (1/10)")
        String description,

        @Schema(description = "ID do grupo de parcelamento (se fizer parte de compra parcelada)", example = "d94b15bb-09c3-4d43-9824-76ec5a7cf11e")
        UUID installmentGroupId,

        @Schema(description = "Número desta parcela na compra", example = "1")
        Integer installmentNumber,

        @Schema(description = "Total de parcelas da compra", example = "10")
        Integer totalInstallments
) {
    public TransactionResponseDTO(
            UUID id,
            BigDecimal amount,
            TransactionType type,
            String category,
            LocalDate date,
            String description
    ) {
        this(id, amount, type, category, date, description, null, null, null);
    }

    public static TransactionResponseDTO fromEntity(Transaction transaction) {
        return new TransactionResponseDTO(
                transaction.getId(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getCategory(),
                transaction.getDate(),
                transaction.getDescription(),
                transaction.getInstallmentGroupId(),
                transaction.getInstallmentNumber(),
                transaction.getTotalInstallments()
        );
    }
}
