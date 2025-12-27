package com.isaacggr.finance_app.service;

import com.isaacggr.finance_app.domain.entity.Transaction;
import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.domain.enums.TransactionCategory;
import com.isaacggr.finance_app.domain.enums.TransactionType;
import com.isaacggr.finance_app.exception.ConflictException;
import com.isaacggr.finance_app.exception.NotFoundException;
import com.isaacggr.finance_app.repository.TransactionRepository;
import com.isaacggr.finance_app.repository.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
@Transactional
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public TransactionService(
            TransactionRepository transactionRepository,
            UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    // ======================
    // CREATE
    // ======================
    public Transaction create(
        UUID userId,
        BigDecimal amount,
        TransactionType type,
        TransactionCategory category,
        LocalDate date,
        String description
) {
    User user = findActiveUser(userId);

    // ✅ REGRA NO SERVICE
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
        throw new ConflictException("Valor da transação deve ser maior que zero");
    }

    if (date == null) {
        throw new ConflictException("Data da transação é obrigatória");
    }

    Transaction transaction = new Transaction(
            user,
            amount,
            type,
            category,
            date,
            description
    );

    return transactionRepository.save(transaction);
}


    // ======================
    // FIND BY ID
    // ======================
    @Transactional(readOnly = true)
    public Transaction findById(UUID transactionId) {
        return findActiveTransaction(transactionId);
    }

    // ======================
    // LIST (paginated)
    // ======================
    @Transactional(readOnly = true)
    public Page<Transaction> listByUser(UUID userId, Pageable pageable) {
        User user = findActiveUser(userId);

        return transactionRepository
                .findByUserIdAndDeletedFalse(user.getId(), pageable);
    }

    // ======================
    // DELETE (soft)
    // ======================
    public void delete(UUID transactionId) {
        Transaction transaction = findActiveTransaction(transactionId);

        if (transaction.isDeleted()) {
            throw new ConflictException("Transação já foi deletada");
        }

        transaction.markAsDeleted();
    }

    // ======================
    // MÉTODOS PRIVADOS
    // ======================
    private User findActiveUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));

        if (user.isDeleted()) {
            throw new ConflictException("Usuário está deletado");
        }

        return user;
    }

    private Transaction findActiveTransaction(UUID transactionId) {
        return transactionRepository
                .findByIdAndDeletedFalse(transactionId)
                .orElseThrow(() -> new NotFoundException("Transação não encontrada"));
    }

}
