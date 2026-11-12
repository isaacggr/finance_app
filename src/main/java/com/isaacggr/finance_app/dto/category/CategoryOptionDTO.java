package com.isaacggr.finance_app.dto.category;

import com.isaacggr.finance_app.domain.enums.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;

public record CategoryOptionDTO(
        @Schema(description = "Nome normalizado salvo nas transações, orçamentos e contas", example = "ALIMENTAÇÃO")
        String name,

        @Schema(description = "Rótulo legível acentuado para exibição na interface", example = "Alimentação")
        String label,

        @Schema(description = "Tipo sugerido (INCOME, EXPENSE ou null quando aplicável a ambos)", example = "EXPENSE")
        TransactionType suggestedType,

        @Schema(description = "Indica se faz parte do catálogo padrão sugerido pelo sistema", example = "true")
        boolean defaultSuggested,

        @Schema(description = "Indica se o usuário autenticado já utilizou esta categoria no seu histórico", example = "true")
        boolean usedByUser
) {
}
