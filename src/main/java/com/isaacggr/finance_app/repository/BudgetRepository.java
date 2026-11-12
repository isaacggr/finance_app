package com.isaacggr.finance_app.repository;

import com.isaacggr.finance_app.domain.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    // 🔍 Buscar por ID e ID do Usuário (Prevenção estrita de IDOR)
    Optional<Budget> findByIdAndUserId(UUID id, UUID userId);

    // 📅 Listar todos os orçamentos do usuário para um determinado mês e ano
    List<Budget> findByUserIdAndMonthAndYear(UUID userId, int month, int year);

    // 🔒 Verificar se já existe orçamento para a mesma categoria no mesmo mês/ano
    boolean existsByUserIdAndCategoryAndMonthAndYear(
            UUID userId,
            String category,
            int month,
            int year
    );

    // 🔒 Verificar conflito de unicidade ao atualizar orçamento (ignorando o próprio ID)
    boolean existsByUserIdAndCategoryAndMonthAndYearAndIdNot(
            UUID userId,
            String category,
            int month,
            int year,
            UUID id
    );

    Optional<Budget> findByUserIdAndCategoryAndMonthAndYear(
            UUID userId,
            String category,
            int month,
            int year
    );

    @Query("SELECT DISTINCT b.category FROM Budget b WHERE b.user.id = :userId ORDER BY b.category ASC")
    List<String> findDistinctCategoriesByUserId(@Param("userId") UUID userId);
}
