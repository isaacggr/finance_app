package com.isaacggr.finance_app.dto;

import com.isaacggr.finance_app.domain.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Projeção JPQL agregada por data e tipo para composição eficiente de séries temporais.
 */
public record DailyTransactionAggregateDTO(
        LocalDate date,
        TransactionType type,
        BigDecimal total
) {
}
