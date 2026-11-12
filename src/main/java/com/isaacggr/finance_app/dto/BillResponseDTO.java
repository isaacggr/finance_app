package com.isaacggr.finance_app.dto;

import com.isaacggr.finance_app.domain.entity.Bill;
import com.isaacggr.finance_app.domain.enums.BillRecurrence;
import com.isaacggr.finance_app.domain.enums.BillStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record BillResponseDTO(
        @Schema(description = "Identificador único da conta a pagar", example = "91a85f64-5717-4562-b3fc-2c963f66a111")
        UUID id,

        @Schema(description = "Título da conta", example = "Internet Fibra")
        String title,

        @Schema(description = "Descrição opcional da conta", example = "Fatura mensal com vencimento automático")
        String description,

        @Schema(description = "Categoria normalizada da despesa", example = "MORADIA")
        String category,

        @Schema(description = "Valor da conta em reais", example = "129.90")
        BigDecimal amount,

        @Schema(description = "Data de vencimento (AAAA-MM-DD)", example = "2026-11-15")
        LocalDate dueDate,

        @Schema(description = "Diferença em dias até o vencimento em America/Sao_Paulo (0=hoje, negativo=atrasada)", example = "3")
        long dueInDays,

        @Schema(description = "Recorrência da conta", example = "MONTHLY")
        BillRecurrence recurrence,

        @Schema(description = "Status efetivo da conta (PENDING, PAID ou OVERDUE)", example = "PENDING")
        BillStatus status,

        @Schema(description = "Data em que a conta foi paga (quando PAID)", example = "2026-11-11")
        LocalDate paidAt,

        @Schema(description = "ID da transação de despesa gerada automaticamente no pagamento, se houver", example = "c31f7f64-2b43-4c11-941a-758420c45999")
        UUID paidTransactionId,

        @Schema(description = "Data e hora de criação", example = "2026-11-11T20:50:00")
        LocalDateTime createdAt,

        @Schema(description = "Data e hora da última atualização", example = "2026-11-11T20:50:00")
        LocalDateTime updatedAt
) {
    public static BillResponseDTO fromEntity(Bill bill) {
        LocalDate today = LocalDate.now(Bill.DEFAULT_ZONE);
        return new BillResponseDTO(
                bill.getId(),
                bill.getTitle(),
                bill.getDescription(),
                bill.getCategory(),
                bill.getAmount(),
                bill.getDueDate(),
                bill.getDueInDaysOn(today),
                bill.getRecurrence(),
                bill.getStatusOn(today),
                bill.getPaidAt(),
                bill.getPaidTransactionId(),
                bill.getCreatedAt(),
                bill.getUpdatedAt()
        );
    }
}
