package com.isaacggr.finance_app.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.isaacggr.finance_app.domain.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Optional<User> findByIdAndDeletedFalse(UUID id);

    List<User> findAllByDeletedFalse();

    List<User> findByEmailVerifiedTrueAndDeletedFalse();

    // 🧹 Expurgo físico definitivo de contas encerradas além da janela de retenção (30 dias)
    @Modifying
    @Query("DELETE FROM User u WHERE u.deleted = true AND u.deletedAt IS NOT NULL AND u.deletedAt <= :cutoff")
    int deleteDeletedUsersBefore(@Param("cutoff") LocalDateTime cutoff);
}
