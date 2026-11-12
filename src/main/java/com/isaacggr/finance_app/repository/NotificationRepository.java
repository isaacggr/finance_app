package com.isaacggr.finance_app.repository;

import com.isaacggr.finance_app.domain.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Optional<Notification> findByIdAndUserId(UUID id, UUID userId);

    Page<Notification> findByUserId(UUID userId, Pageable pageable);

    Page<Notification> findByUserIdAndReadFalse(UUID userId, Pageable pageable);

    long countByUserIdAndReadFalse(UUID userId);

    boolean existsByUserIdAndDedupKey(UUID userId, String dedupKey);

    @Modifying
    @Query("""
        UPDATE Notification n
        SET n.read = true, n.readAt = :readAt
        WHERE n.user.id = :userId
          AND n.read = false
    """)
    int markAllAsReadByUserId(@Param("userId") UUID userId, @Param("readAt") LocalDateTime readAt);
}
