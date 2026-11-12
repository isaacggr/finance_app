package com.isaacggr.finance_app.service;

import com.isaacggr.finance_app.domain.entity.Transaction;
import com.isaacggr.finance_app.domain.enums.TransactionType;
import com.isaacggr.finance_app.dto.category.CategoryOptionDTO;
import com.isaacggr.finance_app.repository.BillRepository;
import com.isaacggr.finance_app.repository.BudgetRepository;
import com.isaacggr.finance_app.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private record DefaultCategory(String name, String label, TransactionType suggestedType) {
    }

    private static final List<DefaultCategory> DEFAULT_CATEGORIES = List.of(
            new DefaultCategory("ALIMENTAÇÃO", "Alimentação", TransactionType.EXPENSE),
            new DefaultCategory("MORADIA", "Moradia", TransactionType.EXPENSE),
            new DefaultCategory("TRANSPORTE", "Transporte", TransactionType.EXPENSE),
            new DefaultCategory("SAÚDE", "Saúde", TransactionType.EXPENSE),
            new DefaultCategory("EDUCAÇÃO", "Educação", TransactionType.EXPENSE),
            new DefaultCategory("LAZER", "Lazer", TransactionType.EXPENSE),
            new DefaultCategory("COMPRAS", "Compras", TransactionType.EXPENSE),
            new DefaultCategory("ASSINATURAS", "Assinaturas", TransactionType.EXPENSE),
            new DefaultCategory("PETS", "Pets", TransactionType.EXPENSE),
            new DefaultCategory("SERVIÇOS", "Serviços", TransactionType.EXPENSE),
            new DefaultCategory("SALÁRIO", "Salário", TransactionType.INCOME),
            new DefaultCategory("FREELANCE", "Freelance", TransactionType.INCOME),
            new DefaultCategory("INVESTIMENTOS", "Investimentos", TransactionType.INCOME),
            new DefaultCategory("VENDAS", "Vendas", TransactionType.INCOME),
            new DefaultCategory("REEMBOLSO", "Reembolso", TransactionType.INCOME),
            new DefaultCategory("OUTROS", "Outros", null)
    );

    private static final Set<String> LOWERCASE_CONNECTORS = Set.of("de", "da", "do", "das", "dos", "e", "em", "para");
    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final BillRepository billRepository;

    public CategoryService(
            TransactionRepository transactionRepository,
            BudgetRepository budgetRepository,
            BillRepository billRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.billRepository = billRepository;
    }

    /**
     * Combina sugestões padrão acentuadas com categorias já utilizadas pelo usuário,
     * preservando total liberdade para digitação de novas categorias customizadas.
     */
    public List<CategoryOptionDTO> listCategories(UUID userId, TransactionType typeFilter) {
        Set<String> usedCategories = collectUsedCategories(userId, typeFilter);
        Set<String> defaultNames = new HashSet<>();
        List<CategoryOptionDTO> result = new ArrayList<>();

        for (DefaultCategory suggestion : DEFAULT_CATEGORIES) {
            defaultNames.add(suggestion.name());
            if (matchesFilter(suggestion, typeFilter)) {
                result.add(new CategoryOptionDTO(
                        suggestion.name(),
                        suggestion.label(),
                        suggestion.suggestedType(),
                        true,
                        usedCategories.contains(suggestion.name())
                ));
            }
        }

        List<String> customCategories = usedCategories.stream()
                .filter(name -> !defaultNames.contains(name))
                .sorted()
                .toList();

        for (String customName : customCategories) {
            result.add(new CategoryOptionDTO(
                    customName,
                    formatCustomLabel(customName),
                    typeFilter,
                    false,
                    true
            ));
        }

        return result;
    }

    private Set<String> collectUsedCategories(UUID userId, TransactionType typeFilter) {
        Set<String> used = new HashSet<>();

        List<String> transactionCategories = (typeFilter == null)
                ? transactionRepository.findDistinctCategoriesByUserId(userId)
                : transactionRepository.findDistinctCategoriesByUserIdAndType(userId, typeFilter);

        addNormalizedCategories(used, transactionCategories);

        if (typeFilter == null || typeFilter == TransactionType.EXPENSE) {
            addNormalizedCategories(used, budgetRepository.findDistinctCategoriesByUserId(userId));
            addNormalizedCategories(used, billRepository.findDistinctCategoriesByUserId(userId));
        }

        return used;
    }

    private void addNormalizedCategories(Set<String> target, List<String> categories) {
        if (categories == null) {
            return;
        }
        for (String category : categories) {
            if (category != null && !category.isBlank()) {
                target.add(Transaction.normalizeCategory(category));
            }
        }
    }

    private boolean matchesFilter(DefaultCategory suggestion, TransactionType typeFilter) {
        return typeFilter == null
                || suggestion.suggestedType() == null
                || suggestion.suggestedType() == typeFilter;
    }

    private String formatCustomLabel(String normalizedName) {
        String lower = normalizedName.toLowerCase(PT_BR).replace('_', ' ');
        String[] words = lower.trim().split("\\s+");
        StringBuilder formatted = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (word.isEmpty()) {
                continue;
            }
            if (!formatted.isEmpty()) {
                formatted.append(' ');
            }
            if (i > 0 && LOWERCASE_CONNECTORS.contains(word)) {
                formatted.append(word);
            } else {
                formatted.append(word.substring(0, 1).toUpperCase(PT_BR))
                        .append(word.substring(1));
            }
        }

        return formatted.isEmpty() ? normalizedName : formatted.toString();
    }
}
