package com.isaacggr.finance_app.domain.entity;

import com.isaacggr.finance_app.domain.enums.QuestCategory;
import com.isaacggr.finance_app.domain.enums.QuestStatus;
import com.isaacggr.finance_app.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_quests", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_quest_period", columnNames = {"user_id", "quest_code", "period_key"})
}, indexes = {
        @Index(name = "idx_user_quests_user_status", columnList = "user_id, status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserQuest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(name = "quest_code", nullable = false, length = 50)
    private String questCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestCategory category;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(name = "xp_reward", nullable = false)
    private int xpReward;

    @Column(name = "current_progress", nullable = false)
    private int currentProgress = 0;

    @Column(name = "target_progress", nullable = false)
    private int targetProgress = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestStatus status = QuestStatus.IN_PROGRESS;

    @Column(name = "period_key", nullable = false, length = 30)
    private String periodKey;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public UserQuest(
            User user,
            String questCode,
            QuestCategory category,
            String title,
            String description,
            int xpReward,
            int targetProgress,
            String periodKey,
            LocalDateTime expiresAt
    ) {
        this.user = user;
        this.questCode = questCode;
        this.category = category;
        this.title = title;
        this.description = description;
        this.xpReward = xpReward;
        this.currentProgress = 0;
        this.targetProgress = Math.max(1, targetProgress);
        this.status = QuestStatus.IN_PROGRESS;
        this.periodKey = periodKey;
        this.expiresAt = expiresAt;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Incrementa o progresso da missão. Se atingir a meta, conclui a quest.
     */
    public boolean incrementProgress(int amount) {
        if (this.status != QuestStatus.IN_PROGRESS || amount <= 0) {
            return false;
        }

        this.currentProgress = Math.min(this.targetProgress, this.currentProgress + amount);
        this.updatedAt = LocalDateTime.now();

        if (this.currentProgress >= this.targetProgress) {
            this.status = QuestStatus.COMPLETED;
            this.completedAt = LocalDateTime.now();
            return true;
        }
        return false;
    }

    /**
     * Define o progresso absoluto da missão (ex: porcentagem ou contagem direta).
     */
    public boolean setProgress(int progress) {
        if (this.status != QuestStatus.IN_PROGRESS) {
            return false;
        }

        this.currentProgress = Math.min(this.targetProgress, Math.max(0, progress));
        this.updatedAt = LocalDateTime.now();

        if (this.currentProgress >= this.targetProgress) {
            this.status = QuestStatus.COMPLETED;
            this.completedAt = LocalDateTime.now();
            return true;
        }
        return false;
    }

    /**
     * Resgata o XP da missão concluída.
     */
    public int claim() {
        if (this.status == QuestStatus.CLAIMED) {
            throw new BusinessException("Esta recompensa já foi resgatada anteriormente.");
        }
        if (this.status != QuestStatus.COMPLETED) {
            throw new BusinessException("A missão ainda não foi concluída para que a recompensa seja resgatada.");
        }

        this.status = QuestStatus.CLAIMED;
        this.claimedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        return this.xpReward;
    }
}
