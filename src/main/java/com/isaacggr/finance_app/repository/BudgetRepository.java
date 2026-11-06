package com.isaacggr.finance_app.repository;

import com.isaacggr.finance_app.domain.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    // 🔍 Buscar por ID e ID do Usuário (Prevenção estrita de IDOR)
    Optional<Budget> findByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);

    // 📅 Listar todos os orçamentos ativos do usuário para um determinado mês e ano
    List<Budget> findByUserIdAndMonthAndYearAndDeletedFalse(UUID userId, int month, int year);

    // 🔒 Verificar se já existe orçamento ativo para a mesma categoria no mesmo mês/ano
    boolean existsByUserIdAndCategoryAndMonthAndYearAndDeletedFalse(
            UUID userId,
            String category,
            int month,
            int year
    );
}
