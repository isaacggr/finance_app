package com.isaacggr.finance_app.repository;

import com.isaacggr.finance_app.domain.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    // 🔍 Buscar transação ativa por ID e ID do Usuário (Prevenção de IDOR)
    Optional<Transaction> findByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);

    // 🔍 Buscar transação ativa apenas por ID (caso administrativo/geral)
    Optional<Transaction> findByIdAndDeletedFalse(UUID id);

    // 📄 Listar transações ativas de um usuário (paginação)
    Page<Transaction> findByUserIdAndDeletedFalse(UUID userId, Pageable pageable);
}
