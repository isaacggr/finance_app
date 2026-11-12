package com.isaacggr.finance_app.domain.entity;

import com.isaacggr.finance_app.domain.enums.NotificationType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications",
        indexes = {
                @Index(name = "idx_notifications_user_read_created", columnList = "user_id, is_read, created_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_notifications_user_dedup", columnNames = {"user_id", "dedup_key"})
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    private static final int MAX_TITLE_LENGTH = 120;
    private static final int MAX_MESSAGE_LENGTH = 500;
    private static final int MAX_DEDUP_KEY_LENGTH = 120;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "reference_type", length = 30)
    private String referenceType;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "dedup_key", length = 120)
    private String dedupKey;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Notification(
            User user,
            NotificationType type,
            String title,
            String message,
            String referenceType,
            UUID referenceId,
            String dedupKey
    ) {
        this.user = user;
        this.type = type;
        this.title = truncate(title, MAX_TITLE_LENGTH);
        this.message = truncate(message, MAX_MESSAGE_LENGTH);
        this.referenceType = (referenceType != null && !referenceType.isBlank()) ? referenceType.trim() : null;
        this.referenceId = referenceId;
        this.dedupKey = (dedupKey != null && !dedupKey.isBlank()) ? truncate(dedupKey.trim(), MAX_DEDUP_KEY_LENGTH) : null;
        this.read = false;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public void markAsRead() {
        if (!this.read) {
            this.read = true;
            this.readAt = LocalDateTime.now();
        }
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return (trimmed.length() > maxLength) ? trimmed.substring(0, maxLength) : trimmed;
    }
}
