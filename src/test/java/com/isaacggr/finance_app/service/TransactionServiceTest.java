package com.isaacggr.finance_app.service;

import com.isaacggr.finance_app.domain.entity.Transaction;
import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.domain.enums.TransactionType;
import com.isaacggr.finance_app.exception.ConflictException;
import com.isaacggr.finance_app.exception.NotFoundException;
import com.isaacggr.finance_app.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BudgetAlertEvaluator budgetAlertEvaluator;

    @InjectMocks
    private TransactionService transactionService;

    private User sampleUser;
    private Transaction sampleTransaction;
    private UUID userId;
    private UUID transactionId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        transactionId = UUID.randomUUID();

        sampleUser = new User("Isaac Gregorio", "isaac@example.com", "pass1234");
        ReflectionTestUtils.setField(sampleUser, "id", userId);

        sampleTransaction = new Transaction(
                sampleUser,
                new BigDecimal("150.00"),
                TransactionType.EXPENSE,
                "ALIMENTACAO",
                LocalDate.now(),
                "Almoço de negócios"
        );
        ReflectionTestUtils.setField(sampleTransaction, "id", transactionId);
    }

    @Test
    @DisplayName("Deve criar transação com sucesso e normalizar categoria em maiúsculas")
    void create_Success_NormalizesCategory() {
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        Transaction created = transactionService.create(
                sampleUser,
                new BigDecimal("150.00"),
                TransactionType.EXPENSE,
                "  pets e veterinario  ",
                LocalDate.now(),
                "Vacina"
        );

        assertNotNull(created);
        assertEquals(new BigDecimal("150.00"), created.getAmount());
        assertEquals(TransactionType.EXPENSE, created.getType());
        assertEquals("PETS E VETERINARIO", created.getCategory());
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Deve lançar NotFoundException ao criar transação quando usuário for nulo ou inativo")
    void create_ThrowsNotFoundException_WhenUserIsNullOrDeleted() {
        assertThrows(NotFoundException.class, () -> transactionService.create(
                null,
                new BigDecimal("100.00"),
                TransactionType.INCOME,
                "OUTROS",
                LocalDate.now(),
                "Depósito"
        ));

        sampleUser.markAsDeleted();
        assertThrows(NotFoundException.class, () -> transactionService.create(
                sampleUser,
                new BigDecimal("100.00"),
                TransactionType.INCOME,
                "OUTROS",
                LocalDate.now(),
                "Depósito"
        ));
    }

    @Test
    @DisplayName("Deve lançar ConflictException quando o valor da transação for menor ou igual a zero")
    void create_ThrowsConflictException_WhenAmountIsZeroOrNegative() {
        assertThrows(ConflictException.class, () -> transactionService.create(
                sampleUser,
                BigDecimal.ZERO,
                TransactionType.INCOME,
                "OUTROS",
                LocalDate.now(),
                "Depósito"
        ));

        assertThrows(ConflictException.class, () -> transactionService.create(
                sampleUser,
                new BigDecimal("-50.00"),
                TransactionType.EXPENSE,
                "ALIMENTACAO",
                LocalDate.now(),
                "Despesa"
        ));
    }

    @Test
    @DisplayName("Deve buscar uma transação por ID garantindo pertencimento ao usuário")
    void findById_Success() {
        when(transactionRepository.findByIdAndUserId(transactionId, userId))
                .thenReturn(Optional.of(sampleTransaction));

        Transaction found = transactionService.findById(transactionId, userId);

        assertNotNull(found);
        assertEquals(sampleTransaction.getAmount(), found.getAmount());
    }

    @Test
    @DisplayName("Deve lançar NotFoundException quando a transação não existir ou pertencer a outro usuário")
    void findById_ThrowsNotFoundException_WhenNotFoundOrBelongsToAnotherUser() {
        when(transactionRepository.findByIdAndUserId(transactionId, userId))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> transactionService.findById(transactionId, userId));
    }

    @Test
    @DisplayName("Deve listar transações com e sem filtros usando ordenação estável padrão")
    void listByUser_UsesDefaultStableSort_WithAndWithoutFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        Pageable expectedPageable = PageRequest.of(
                0,
                10,
                Sort.by(Sort.Order.desc("date"), Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );
        when(transactionRepository.findAll(ArgumentMatchers.<Specification<Transaction>>any(), eq(expectedPageable)))
                .thenReturn(new PageImpl<>(List.of(sampleTransaction)));

        Page<Transaction> unfiltered = transactionService.listByUser(userId, pageable);
        Page<Transaction> filtered = transactionService.listByUser(
                userId,
                LocalDate.now().minusDays(5),
                LocalDate.now(),
                "ALIMENTACAO",
                TransactionType.EXPENSE,
                pageable
        );

        assertEquals(1, unfiltered.getTotalElements());
        assertEquals(1, filtered.getTotalElements());
    }

    @Test
    @DisplayName("Deve preservar ordenação customizada e acrescentar desempate estável (createdAt desc, id desc)")
    void listByUser_PreservesCustomSortAndAppendsTieBreakers() {
        Pageable customPageable = PageRequest.of(1, 20, Sort.by(Sort.Order.asc("amount")));
        when(transactionRepository.findAll(ArgumentMatchers.<Specification<Transaction>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleTransaction)));

        transactionService.listByUser(userId, customPageable);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(transactionRepository).findAll(ArgumentMatchers.<Specification<Transaction>>any(), pageableCaptor.capture());

        Pageable usedPageable = pageableCaptor.getValue();
        assertEquals(1, usedPageable.getPageNumber());
        assertEquals(20, usedPageable.getPageSize());
        assertEquals(
                Sort.by(Sort.Order.asc("amount"), Sort.Order.desc("createdAt"), Sort.Order.desc("id")),
                usedPageable.getSort()
        );
    }

    @Test
    @DisplayName("Deve normalizar valores monetários com escala 2 usando RoundingMode.HALF_EVEN")
    void normalizeMonetaryAmount_UsesHalfEvenScale2() {
        assertEquals(new BigDecimal("10.00"), Transaction.normalizeMonetaryAmount(new BigDecimal("10.005")));
        assertEquals(new BigDecimal("10.02"), Transaction.normalizeMonetaryAmount(new BigDecimal("10.015")));
        assertEquals(new BigDecimal("89.90"), Transaction.normalizeMonetaryAmount(new BigDecimal("89.9")));
        assertNull(Transaction.normalizeMonetaryAmount(null));
    }

    @Test
    @DisplayName("Deve lançar ConflictException quando data inicial for posterior à data final no filtro")
    void listByUser_ThrowsConflict_WhenStartDateIsAfterEndDate() {
        Pageable pageable = PageRequest.of(0, 10);
        LocalDate start = LocalDate.now();
        LocalDate end = LocalDate.now().minusDays(1);

        assertThrows(ConflictException.class, () -> transactionService.listByUser(
                userId,
                start,
                end,
                null,
                null,
                pageable
        ));
    }

    @Test
    @DisplayName("Deve excluir transação do usuário definitivamente (Hard Delete)")
    void delete_Success() {
        when(transactionRepository.findByIdAndUserId(transactionId, userId))
                .thenReturn(Optional.of(sampleTransaction));

        transactionService.delete(transactionId, userId);

        verify(transactionRepository, times(1)).delete(sampleTransaction);
    }

    @Test
    @DisplayName("Deve atualizar transação do usuário com sucesso")
    void update_Success() {
        when(transactionRepository.findByIdAndUserId(transactionId, userId))
                .thenReturn(Optional.of(sampleTransaction));

        LocalDate newDate = LocalDate.now().plusDays(1);
        Transaction updated = transactionService.update(
                transactionId,
                userId,
                new BigDecimal("299.90"),
                TransactionType.INCOME,
                "INVESTIMENTOS",
                newDate,
                "Rendimento de investimentos"
        );

        assertNotNull(updated);
        assertEquals(new BigDecimal("299.90"), updated.getAmount());
        assertEquals(TransactionType.INCOME, updated.getType());
        assertEquals("INVESTIMENTOS", updated.getCategory());
        assertEquals(newDate, updated.getDate());
        assertEquals("Rendimento de investimentos", updated.getDescription());
    }

    @Test
    @DisplayName("Deve lançar NotFoundException ao tentar atualizar transação inexistente ou de outro usuário")
    void update_ThrowsNotFound_WhenNotFoundOrNotOwned() {
        when(transactionRepository.findByIdAndUserId(transactionId, userId))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> transactionService.update(
                transactionId,
                userId,
                new BigDecimal("100.00"),
                TransactionType.EXPENSE,
                "OUTROS",
                LocalDate.now(),
                "Atualização"
        ));
    }

    @Test
    @DisplayName("Deve lançar ConflictException ao tentar atualizar transação com valor inválido ou sem data")
    void update_ThrowsConflict_WhenAmountOrDateIsInvalid() {
        when(transactionRepository.findByIdAndUserId(transactionId, userId))
                .thenReturn(Optional.of(sampleTransaction));

        assertThrows(ConflictException.class, () -> transactionService.update(
                transactionId, userId, BigDecimal.ZERO, TransactionType.EXPENSE, "OUTROS", LocalDate.now(), "Atualização"
        ));
        assertThrows(ConflictException.class, () -> transactionService.update(
                transactionId, userId, new BigDecimal("-10.00"), TransactionType.EXPENSE, "OUTROS", LocalDate.now(), "Atualização"
        ));
        assertThrows(ConflictException.class, () -> transactionService.update(
                transactionId, userId, new BigDecimal("50.00"), TransactionType.EXPENSE, "OUTROS", null, "Atualização"
        ));
    }
}
