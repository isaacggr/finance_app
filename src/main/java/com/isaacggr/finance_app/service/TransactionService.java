package com.isaacggr.finance_app.service;

import com.isaacggr.finance_app.domain.entity.Transaction;
import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.domain.enums.TransactionType;
import com.isaacggr.finance_app.exception.ConflictException;
import com.isaacggr.finance_app.exception.NotFoundException;
import com.isaacggr.finance_app.repository.TransactionRepository;
import com.isaacggr.finance_app.repository.specification.TransactionSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    // ======================
    // CREATE (Associado ao Usuário Autenticado)
    // ======================
    public Transaction create(
            User user,
            BigDecimal amount,
            TransactionType type,
            String category,
            LocalDate date,
            String description
    ) {
        if (user == null || !user.isActive()) {
            throw new NotFoundException("Usuário não encontrado");
        }

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
    // FIND BY ID (Garante que a transação pertence ao usuário)
    // ======================
    @Transactional(readOnly = true)
    public Transaction findById(UUID transactionId, UUID userId) {
        return transactionRepository
                .findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new NotFoundException("Transação não encontrada"));
    }

    // ======================
    // LIST (Transações do Usuário Autenticado com Filtros Dinâmicos)
    // ======================
    @Transactional(readOnly = true)
    public Page<Transaction> listByUser(
            UUID userId,
            LocalDate startDate,
            LocalDate endDate,
            String category,
            TransactionType type,
            Pageable pageable
    ) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ConflictException("A data inicial não pode ser posterior à data final");
        }

        Specification<Transaction> spec = TransactionSpecification.withFilters(
                userId,
                startDate,
                endDate,
                category,
                type
        );

        return transactionRepository.findAll(spec, withStableSort(pageable));
    }

    private Pageable withStableSort(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, 15, Sort.by(Sort.Order.desc("date"), Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        }
        if (pageable.isUnpaged()) {
            return pageable;
        }

        List<Sort.Order> orders = new ArrayList<>();
        Sort currentSort = pageable.getSort();
        if (currentSort.isUnsorted()) {
            orders.add(Sort.Order.desc("date"));
        } else {
            currentSort.forEach(orders::add);
        }

        if (currentSort.getOrderFor("createdAt") == null) {
            orders.add(Sort.Order.desc("createdAt"));
        }
        if (currentSort.getOrderFor("id") == null) {
            orders.add(Sort.Order.desc("id"));
        }

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(orders));
    }

    @Transactional(readOnly = true)
    public Page<Transaction> listByUser(UUID userId, Pageable pageable) {
        return listByUser(userId, null, null, null, null, pageable);
    }

    // ======================
    // UPDATE (Atualiza transação pertencente ao usuário autenticado)
    // ======================
    public Transaction update(
            UUID transactionId,
            UUID userId,
            BigDecimal amount,
            TransactionType type,
            String category,
            LocalDate date,
            String description
    ) {
        Transaction transaction = findById(transactionId, userId);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ConflictException("Valor da transação deve ser maior que zero");
        }

        if (date == null) {
            throw new ConflictException("Data da transação é obrigatória");
        }

        transaction.update(amount, type, category, date, description);
        return transaction;
    }

    // ======================
    // DELETE (Hard delete definitivo da transação do usuário)
    // ======================
    public void delete(UUID transactionId, UUID userId) {
        Transaction transaction = findById(transactionId, userId);
        transactionRepository.delete(transaction);
    }
}
