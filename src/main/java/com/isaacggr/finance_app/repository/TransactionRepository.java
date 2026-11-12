package com.isaacggr.finance_app.repository;

import com.isaacggr.finance_app.domain.entity.Transaction;
import com.isaacggr.finance_app.domain.enums.TransactionType;
import com.isaacggr.finance_app.dto.dashboard.CategoryExpenseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    // 🔍 Buscar transação por ID e ID do Usuário (Prevenção de IDOR)
    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

    // 📄 Listar transações de um usuário (paginação simples legada)
    Page<Transaction> findByUserId(UUID userId, Pageable pageable);

    // 📊 Soma total por tipo (INCOME ou EXPENSE) em um intervalo de datas
    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM Transaction t
        WHERE t.user.id = :userId
          AND t.type = :type
          AND t.date >= :startDate
          AND t.date <= :endDate
    """)
    BigDecimal sumAmountByTypeAndPeriod(
            @Param("userId") UUID userId,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // 📊 Detalhamento de gastos por categoria em um intervalo de datas
    @Query("""
        SELECT new com.isaacggr.finance_app.dto.dashboard.CategoryExpenseDTO(t.category, SUM(t.amount))
        FROM Transaction t
        WHERE t.user.id = :userId
          AND t.type = :type
          AND t.date >= :startDate
          AND t.date <= :endDate
        GROUP BY t.category
        ORDER BY SUM(t.amount) DESC
    """)
    List<CategoryExpenseDTO> sumAmountByCategory(
            @Param("userId") UUID userId,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM Transaction t
        WHERE t.user.id = :userId
          AND t.type = :type
          AND t.category = :category
          AND t.date >= :startDate
          AND t.date <= :endDate
    """)
    BigDecimal sumAmountByTypeAndCategoryAndPeriod(
            @Param("userId") UUID userId,
            @Param("type") TransactionType type,
            @Param("category") String category,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
        SELECT new com.isaacggr.finance_app.repository.TransactionRepository$DailyAggregate(t.date, t.type, SUM(t.amount))
        FROM Transaction t
        WHERE t.user.id = :userId
          AND t.date >= :startDate
          AND t.date <= :endDate
        GROUP BY t.date, t.type
        ORDER BY t.date ASC
    """)
    List<DailyAggregate> aggregateByDateAndType(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT DISTINCT t.category FROM Transaction t WHERE t.user.id = :userId ORDER BY t.category ASC")
    List<String> findDistinctCategoriesByUserId(@Param("userId") UUID userId);

    @Query("SELECT DISTINCT t.category FROM Transaction t WHERE t.user.id = :userId AND t.type = :type ORDER BY t.category ASC")
    List<String> findDistinctCategoriesByUserIdAndType(
            @Param("userId") UUID userId,
            @Param("type") TransactionType type
    );

    /**
     * Projeção JPQL agregada por data e tipo para composição eficiente de séries temporais.
     */
    record DailyAggregate(
            LocalDate date,
            TransactionType type,
            BigDecimal total
    ) {
    }
}
