package com.isaacggr.finance_app.repository;

import com.isaacggr.finance_app.domain.entity.InstallmentGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InstallmentGroupRepository extends JpaRepository<InstallmentGroup, UUID> {

    Optional<InstallmentGroup> findByIdAndUserId(UUID id, UUID userId);

    @Query("""
        SELECT ig FROM InstallmentGroup ig
        WHERE ig.user.id = :userId
        ORDER BY ig.startDate DESC, ig.createdAt DESC
    """)
    Page<InstallmentGroup> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("""
        SELECT ig FROM InstallmentGroup ig
        WHERE ig.user.id = :userId
        ORDER BY ig.startDate DESC, ig.createdAt DESC
    """)
    List<InstallmentGroup> findByUserId(@Param("userId") UUID userId);
}
