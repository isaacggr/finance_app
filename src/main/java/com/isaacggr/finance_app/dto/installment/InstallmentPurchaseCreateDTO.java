package com.isaacggr.finance_app.dto.installment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Dados para criação de uma compra parcelada no cartão")
public record InstallmentPurchaseCreateDTO(

        @Schema(description = "Valor total da compra parcelada (opcional caso installmentAmount seja fornecido)", example = "1200.00")
        @Positive(message = "O valor total da compra deve ser positivo")
        @DecimalMin(value = "0.01", message = "O valor total da compra deve ser de no mínimo 0.01")
        @DecimalMax(value = "9999999999999.99", message = "O valor total da compra deve ser no máximo 9999999999999.99")
        @Digits(integer = 13, fraction = 2, message = "O valor total da compra deve ter no máximo 13 dígitos inteiros e 2 casas decimais")
        BigDecimal totalAmount,

        @Schema(description = "Valor individual de cada parcela (opcional caso totalAmount seja fornecido)", example = "120.00")
        @Positive(message = "O valor da parcela deve ser positivo")
        @DecimalMin(value = "0.01", message = "O valor da parcela deve ser de no mínimo 0.01")
        @DecimalMax(value = "9999999999999.99", message = "O valor da parcela deve ser no máximo 9999999999999.99")
        @Digits(integer = 13, fraction = 2, message = "O valor da parcela deve ter no máximo 13 dígitos inteiros e 2 casas decimais")
        BigDecimal installmentAmount,

        @Schema(description = "Número total de parcelas (mínimo 2)", example = "10")
        @NotNull(message = "O número total de parcelas é obrigatório")
        @Min(value = 2, message = "O número total de parcelas deve ser de no mínimo 2")
        @Max(value = 120, message = "O número total de parcelas deve ser de no máximo 120")
        Integer totalInstallments,

        @Schema(description = "Em qual parcela a compra está atualmente (padrão: 1)", example = "1", defaultValue = "1")
        @Min(value = 1, message = "A parcela atual deve ser de no mínimo 1")
        @Max(value = 120, message = "A parcela atual não pode ser superior a 120")
        Integer currentInstallment,

        @Schema(description = "Categoria da compra (livre, ex: ELETRONICOS, VIAGEM, CARTAO DE CREDITO)", example = "ELETRONICOS")
        @NotBlank(message = "A categoria da compra é obrigatória")
        @Size(max = 30, message = "A categoria deve ter no máximo 30 caracteres")
        String category,

        @Schema(description = "Data do vencimento da primeira parcela ou parcela atual (AAAA-MM-DD)", example = "2026-11-15")
        @NotNull(message = "A data da primeira parcela é obrigatória")
        LocalDate startDate,

        @Schema(description = "Descrição detalhada da compra parcelada", example = "Notebook Dell Inspiron")
        @NotBlank(message = "A descrição da compra parcelada é obrigatória")
        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres")
        String description
) {
}
