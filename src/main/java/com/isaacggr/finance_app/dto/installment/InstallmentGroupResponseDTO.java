package com.isaacggr.finance_app.dto.installment;

import com.isaacggr.finance_app.domain.entity.InstallmentGroup;
import com.isaacggr.finance_app.dto.bill.BillResponseDTO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Dados detalhados de uma compra parcelada e suas parcelas")
public record InstallmentGroupResponseDTO(

        @Schema(description = "Identificador único do grupo de parcelamento", example = "d94b15bb-09c3-4d43-9824-76ec5a7cf11e")
        UUID id,

        @Schema(description = "Descrição da compra parcelada", example = "Notebook Dell Inspiron")
        String description,

        @Schema(description = "Valor total da compra parcelada", example = "1200.00")
        BigDecimal totalAmount,

        @Schema(description = "Número total de parcelas", example = "10")
        Integer totalInstallments,

        @Schema(description = "Categoria da compra parcelada", example = "ELETRONICOS")
        String category,

        @Schema(description = "Data da primeira parcela", example = "2026-11-15")
        LocalDate startDate,

        @Schema(description = "Data de registro da compra parcelada", example = "2026-11-15T14:30:00")
        LocalDateTime createdAt,

        @Schema(description = "Lista detalhada das parcelas a vencer/pagas")
        List<BillResponseDTO> installments
) {
    public static InstallmentGroupResponseDTO fromEntity(InstallmentGroup group) {
        List<BillResponseDTO> list = group.getBills() == null
                ? List.of()
                : group.getBills().stream()
                        .map(BillResponseDTO::fromEntity)
                        .toList();

        return new InstallmentGroupResponseDTO(
                group.getId(),
                group.getDescription(),
                group.getTotalAmount(),
                group.getTotalInstallments(),
                group.getCategory(),
                group.getStartDate(),
                group.getCreatedAt(),
                list
        );
    }
}
