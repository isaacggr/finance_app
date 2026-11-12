package com.isaacggr.finance_app.repository;

import com.isaacggr.finance_app.domain.entity.Bill;
import com.isaacggr.finance_app.domain.enums.BillStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BillRepository extends JpaRepository<Bill, UUID> {

    Optional<Bill> findByIdAndUserId(UUID id, UUID userId);

    @Query("""
        SELECT b FROM Bill b
        WHERE b.user.id = :userId
        ORDER BY CASE WHEN b.status = com.isaacggr.finance_app.domain.enums.BillStatus.PENDING THEN 0 ELSE 1 END ASC,
                 b.dueDate ASC,
                 b.createdAt DESC
    """)
    List<Bill> findByUserIdOrdered(@Param("userId") UUID userId);

    @Query("""
        SELECT b FROM Bill b
        WHERE b.user.id = :userId
          AND b.status = com.isaacggr.finance_app.domain.enums.BillStatus.PENDING
          AND b.dueDate >= :today
        ORDER BY b.dueDate ASC, b.createdAt DESC
    """)
    List<Bill> findUpcomingPendingByUserId(@Param("userId") UUID userId, @Param("today") LocalDate today);

    @Query("""
        SELECT b FROM Bill b
        WHERE b.user.id = :userId
          AND b.status = com.isaacggr.finance_app.domain.enums.BillStatus.PENDING
          AND b.dueDate < :today
        ORDER BY b.dueDate ASC, b.createdAt DESC
    """)
    List<Bill> findOverdueByUserId(@Param("userId") UUID userId, @Param("today") LocalDate today);

    List<Bill> findByUserIdAndStatusOrderByPaidAtDescDueDateDesc(UUID userId, BillStatus status);

    @Query("""
        SELECT b FROM Bill b
        WHERE b.user.id = :userId
          AND b.status = com.isaacggr.finance_app.domain.enums.BillStatus.PENDING
          AND b.dueDate <= :endDate
        ORDER BY b.dueDate ASC, b.createdAt DESC
    """)
    List<Bill> findOpenBillsUpTo(@Param("userId") UUID userId, @Param("endDate") LocalDate endDate);

    List<Bill> findByUserIdAndStatusAndDueDateInOrderByDueDateAscCreatedAtDesc(
            UUID userId,
            BillStatus status,
            Collection<LocalDate> dueDates
    );
}
