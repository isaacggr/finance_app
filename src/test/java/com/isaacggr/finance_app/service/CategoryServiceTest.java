package com.isaacggr.finance_app.service;

import com.isaacggr.finance_app.domain.enums.TransactionType;
import com.isaacggr.finance_app.dto.category.CategoryOptionDTO;
import com.isaacggr.finance_app.repository.BillRepository;
import com.isaacggr.finance_app.repository.BudgetRepository;
import com.isaacggr.finance_app.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private BillRepository billRepository;

    @InjectMocks
    private CategoryService categoryService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Deve combinar sugestões padrão acentuadas com categorias customizadas sem duplicar")
    void listCategories_CombinesDefaultsAndCustomWithoutDuplication() {
        when(transactionRepository.findDistinctCategoriesByUserId(userId))
                .thenReturn(List.of("ALIMENTAÇÃO", "IFOOD", "SALÁRIO"));
        when(budgetRepository.findDistinctCategoriesByUserId(userId))
                .thenReturn(List.of("ALIMENTAÇÃO", "ACADEMIA"));
        when(billRepository.findDistinctCategoriesByUserId(userId))
                .thenReturn(List.of("MORADIA", "CONTA_DE_LUZ"));

        List<CategoryOptionDTO> result = categoryService.listCategories(userId, null);

        CategoryOptionDTO alimentacao = result.stream()
                .filter(c -> c.name().equals("ALIMENTAÇÃO"))
                .findFirst()
                .orElseThrow();
        assertEquals("Alimentação", alimentacao.label());
        assertTrue(alimentacao.defaultSuggested());
        assertTrue(alimentacao.usedByUser());
        assertEquals(TransactionType.EXPENSE, alimentacao.suggestedType());

        CategoryOptionDTO transporte = result.stream()
                .filter(c -> c.name().equals("TRANSPORTE"))
                .findFirst()
                .orElseThrow();
        assertTrue(transporte.defaultSuggested());
        assertFalse(transporte.usedByUser());

        CategoryOptionDTO ifood = result.stream()
                .filter(c -> c.name().equals("IFOOD"))
                .findFirst()
                .orElseThrow();
        assertEquals("Ifood", ifood.label());
        assertFalse(ifood.defaultSuggested());
        assertTrue(ifood.usedByUser());

        CategoryOptionDTO luz = result.stream()
                .filter(c -> c.name().equals("CONTA_DE_LUZ"))
                .findFirst()
                .orElseThrow();
        assertEquals("Conta de Luz", luz.label());
        assertFalse(luz.defaultSuggested());
        assertTrue(luz.usedByUser());

        assertEquals(1, result.stream().filter(c -> c.name().equals("ALIMENTAÇÃO")).count());
    }

    @Test
    @DisplayName("Deve filtrar sugestões e histórico por INCOME sem misturar categorias exclusivas de despesas")
    void listCategories_FiltersByIncome() {
        when(transactionRepository.findDistinctCategoriesByUserIdAndType(userId, TransactionType.INCOME))
                .thenReturn(List.of("SALÁRIO", "DIVIDENDOS_FII"));

        List<CategoryOptionDTO> result = categoryService.listCategories(userId, TransactionType.INCOME);

        assertTrue(result.stream().anyMatch(c -> c.name().equals("SALÁRIO") && c.usedByUser()));
        assertTrue(result.stream().anyMatch(c -> c.name().equals("OUTROS")));
        assertTrue(result.stream().anyMatch(c -> c.name().equals("DIVIDENDOS_FII") && !c.defaultSuggested()));
        assertFalse(result.stream().anyMatch(c -> c.name().equals("ALIMENTAÇÃO")));
        verify(budgetRepository, never()).findDistinctCategoriesByUserId(any());
        verify(billRepository, never()).findDistinctCategoriesByUserId(any());
    }
}
