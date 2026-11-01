package com.isaacggr.finance_app.service;

import com.isaacggr.finance_app.domain.entity.Transaction;
import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.domain.enums.TransactionCategory;
import com.isaacggr.finance_app.domain.enums.TransactionType;
import com.isaacggr.finance_app.exception.ConflictException;
import com.isaacggr.finance_app.exception.NotFoundException;
import com.isaacggr.finance_app.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

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
                TransactionCategory.ALIMENTACAO,
                LocalDate.now(),
                "Almoço de negócios"
        );
        ReflectionTestUtils.setField(sampleTransaction, "id", transactionId);
    }

    @Test
    @DisplayName("Deve criar transação com sucesso para usuário autenticado ativo e dados válidos")
    void create_Success() {
        when(transactionRepository.save(any(Transaction.class))).thenReturn(sampleTransaction);

        Transaction created = transactionService.create(
                sampleUser,
                new BigDecimal("150.00"),
                TransactionType.EXPENSE,
                TransactionCategory.ALIMENTACAO,
                LocalDate.now(),
                "Almoço de negócios"
        );

        assertNotNull(created);
        assertEquals(new BigDecimal("150.00"), created.getAmount());
        assertEquals(TransactionType.EXPENSE, created.getType());
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Deve lançar NotFoundException ao tentar criar transação quando usuário for nulo")
    void create_ThrowsNotFoundException_WhenUserIsNull() {
        assertThrows(NotFoundException.class, () -> transactionService.create(
                null,
                new BigDecimal("100.00"),
                TransactionType.INCOME,
                TransactionCategory.OUTROS,
                LocalDate.now(),
                "Depósito"
        ));
    }

    @Test
    @DisplayName("Deve lançar NotFoundException ao tentar criar transação para usuário inativo")
    void create_ThrowsNotFoundException_WhenUserIsDeleted() {
        sampleUser.markAsDeleted();

        assertThrows(NotFoundException.class, () -> transactionService.create(
                sampleUser,
                new BigDecimal("100.00"),
                TransactionType.INCOME,
                TransactionCategory.OUTROS,
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
                TransactionCategory.OUTROS,
                LocalDate.now(),
                "Depósito"
        ));

        assertThrows(ConflictException.class, () -> transactionService.create(
                sampleUser,
                new BigDecimal("-50.00"),
                TransactionType.EXPENSE,
                TransactionCategory.ALIMENTACAO,
                LocalDate.now(),
                "Despesa"
        ));
    }

    @Test
    @DisplayName("Deve buscar uma transação ativa por ID garantindo pertencimento ao usuário")
    void findById_Success() {
        when(transactionRepository.findByIdAndUserIdAndDeletedFalse(transactionId, userId))
                .thenReturn(Optional.of(sampleTransaction));

        Transaction found = transactionService.findById(transactionId, userId);

        assertNotNull(found);
        assertEquals(sampleTransaction.getAmount(), found.getAmount());
    }

    @Test
    @DisplayName("Deve lançar NotFoundException quando a transação não existir ou pertencer a outro usuário")
    void findById_ThrowsNotFoundException_WhenNotFoundOrBelongsToAnotherUser() {
        when(transactionRepository.findByIdAndUserIdAndDeletedFalse(transactionId, userId))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> transactionService.findById(transactionId, userId));
    }

    @Test
    @DisplayName("Deve listar transações do usuário de forma paginada")
    void listByUser_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        when(transactionRepository.findByUserIdAndDeletedFalse(userId, pageable))
                .thenReturn(new PageImpl<>(List.of(sampleTransaction)));

        Page<Transaction> result = transactionService.listByUser(userId, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("Deve marcar transação do usuário como deletada com sucesso")
    void delete_Success() {
        when(transactionRepository.findByIdAndUserIdAndDeletedFalse(transactionId, userId))
                .thenReturn(Optional.of(sampleTransaction));

        transactionService.delete(transactionId, userId);

        assertTrue(sampleTransaction.isDeleted());
    }
}
