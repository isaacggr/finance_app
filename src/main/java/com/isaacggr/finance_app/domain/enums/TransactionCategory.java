package com.isaacggr.finance_app.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TransactionCategory {
    ALIMENTACAO,
    ALUGUEL,
    TRANSPORTE,
    LAZER,
    CARTAO,
    INVESTIMENTOS,
    EDUCACAO,
    OUTROS;

    @JsonCreator
    public static TransactionCategory fromString(String value) {
        if (value == null) return null;
        String normalized = value.trim().toUpperCase()
                .replace("Ã", "A")
                .replace("Á", "A")
                .replace("Ç", "C")
                .replace("Õ", "O")
                .replace("É", "E");
        for (TransactionCategory category : values()) {
            if (category.name().equals(normalized) || category.name().equalsIgnoreCase(value.trim())) {
                return category;
            }
        }
        throw new IllegalArgumentException("Categoria desconhecida: " + value);
    }
}
