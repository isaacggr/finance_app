package com.isaacggr.finance_app.repository.specification;

import com.isaacggr.finance_app.domain.entity.Transaction;
import com.isaacggr.finance_app.domain.enums.TransactionType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TransactionSpecification {

    public static Specification<Transaction> withFilters(
            UUID userId,
            LocalDate startDate,
            LocalDate endDate,
            String category,
            TransactionType type
    ) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 🔒 Regra Mandatória de Segurança (Zero IDOR)
            predicates.add(builder.equal(root.get("user").get("id"), userId));

            // 📅 Filtros Opcionais por Período
            if (startDate != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("date"), startDate));
            }
            if (endDate != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("date"), endDate));
            }

            // 🏷️ Filtro Opcional por Categoria (Normalizada em maiúsculo)
            if (category != null && !category.isBlank()) {
                predicates.add(builder.equal(root.get("category"), Transaction.normalizeCategory(category)));
            }

            // 🔀 Filtro Opcional por Tipo (INCOME / EXPENSE)
            if (type != null) {
                predicates.add(builder.equal(root.get("type"), type));
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
