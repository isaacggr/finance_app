package com.isaacggr.finance_app.service;

import com.isaacggr.finance_app.domain.entity.Transaction;
import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.domain.enums.TransactionCategory;
import com.isaacggr.finance_app.domain.enums.TransactionType;
import com.isaacggr.finance_app.exception.ConflictException;
import com.isaacggr.finance_app.exception.NotFoundException;
import com.isaacggr.finance_app.repository.TransactionRepository;
import com.isaacggr.finance_app.repository.UserRepository;
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

    @Mock
    private UserRepository userRepository;

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
        sampleTransaction = new Transaction(
                sampleUser,
                new BigDecimal("150.00"),
                TransactionType.EXPENSE,
                TransactionCategory.ALIMENTACAO,
                LocalDate.now(),
                "Almoço de negócios"
        );
    }

    @Test
    @DisplayName("Deve criar transação com sucesso para usuário ativo e dados válidos")
    void create_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(sampleTransaction);

        Transaction created = transactionService.create(
                userId,
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
    @DisplayName("Deve lançar NotFoundException ao tentar criar transação para usuário inexistente")
    void create_ThrowsNotFoundException_WhenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> transactionService.create(
                userId,
                new BigDecimal("100.00"),
                TransactionType.INCOME,
                TransactionCategory.OUTROS,
                LocalDate.now(),
                "Depósito"
        ));
    }

    @Test
    @DisplayName("Deve lançar ConflictException ao tentar criar transação para usuário desativado")
    void create_ThrowsConflictException_WhenUserIsDeleted() {
        sampleUser.markAsDeleted();
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

        assertThrows(ConflictException.class, () -> transactionService.create(
                userId,
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
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

        assertThrows(ConflictException.class, () -> transactionService.create(
                userId,
                BigDecimal.ZERO,
                TransactionType.INCOME,
                TransactionCategory.OUTROS,
                LocalDate.now(),
                "Depósito"
        ));

        assertThrows(ConflictException.class, () -> transactionService.create(
                userId,
                new BigDecimal("-50.00"),
                TransactionType.EXPENSE,
                TransactionCategory.ALIMENTACAO,
                LocalDate.now(),
                "Despesa"
        ));
    }

    @Test
    @DisplayName("Deve buscar uma transação ativa por ID com sucesso")
    void findById_Success() {
        when(transactionRepository.findByIdAndDeletedFalse(transactionId)).thenReturn(Optional.of(sampleTransaction));

        Transaction found = transactionService.findById(transactionId);

        assertNotNull(found);
        assertEquals(sampleTransaction.getAmount(), found.getAmount());
    }

    @Test
    @DisplayName("Deve lançar NotFoundException quando a transação não existir")
    void findById_ThrowsNotFoundException() {
        when(transactionRepository.findByIdAndDeletedFalse(transactionId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> transactionService.findById(transactionId));
    }

    @Test
    @DisplayName("Deve listar transações do usuário de forma paginada")
    void listByUser_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(transactionRepository.findByUserIdAndDeletedFalse(any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(sampleTransaction)));

        Page<Transaction> result = transactionService.listByUser(userId, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("Deve marcar transação como deletada com sucesso")
    void delete_Success() {
        when(transactionRepository.findByIdAndDeletedFalse(transactionId)).thenReturn(Optional.of(sampleTransaction));

        transactionService.delete(transactionId);

        assertTrue(sampleTransaction.isDeleted());
    }
}

