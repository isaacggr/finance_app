package com.isaacggr.goldforge.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_game_profiles", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_game_profiles_user", columnNames = "user_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserGameProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(name = "total_xp", nullable = false)
    private long totalXp = 0;

    @Column(name = "current_level", nullable = false)
    private int currentLevel = 1;

    @Column(name = "streak_days", nullable = false)
    private int streakDays = 0;

    @Column(name = "last_checkin_date")
    private LocalDate lastCheckinDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public UserGameProfile(User user) {
        this.user = user;
        this.totalXp = 0;
        this.currentLevel = 1;
        this.streakDays = 0;
        this.lastCheckinDate = null;
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
     * Adiciona XP ao jogador e recalcula o nível atual.
     * Retorna true caso o jogador tenha subido de nível (Level Up).
     */
    public boolean addXp(long xp) {
        if (xp <= 0) return false;
        int previousLevel = this.currentLevel;
        this.totalXp += xp;
        this.currentLevel = calculateLevelForXp(this.totalXp);
        this.updatedAt = LocalDateTime.now();
        return this.currentLevel > previousLevel;
    }

    /**
     * Registra o check-in diário do jogador atualizando os dias de consistência (streak).
     * Retorna true se um novo check-in foi registrado hoje.
     */
    public boolean recordCheckin(LocalDate today) {
        if (today == null) return false;
        if (this.lastCheckinDate != null && this.lastCheckinDate.equals(today)) {
            return false; // Já realizou check-in hoje
        }

        if (this.lastCheckinDate != null && this.lastCheckinDate.plusDays(1).equals(today)) {
            this.streakDays += 1;
        } else {
            this.streakDays = 1; // Primeiro dia ou quebrou sequência
        }

        this.lastCheckinDate = today;
        this.updatedAt = LocalDateTime.now();
        return true;
    }

    // ==========================================
    // CÁLCULOS MATEMÁTICOS DE NÍVEL & PROGRESSO
    // ==========================================

    /**
     * Retorna o total de XP acumulado necessário para atingir o nível informado.
     * Nível 1: 0 XP
     * Nível 2: 100 XP
     * Nível 3: 300 XP (100 + 200)
     * Nível 4: 600 XP (100 + 200 + 300)
     */
    public static long totalXpForLevel(int level) {
        if (level <= 1) return 0L;
        long l = level;
        return 50L * l * (l - 1);
    }

    /**
     * Calcula o nível correspondente ao total de XP informado.
     */
    public static int calculateLevelForXp(long xp) {
        if (xp <= 0) return 1;
        int level = 1;
        while (xp >= totalXpForLevel(level + 1)) {
            level++;
        }
        return level;
    }

    /** XP acumulado no marco inicial do nível atual. */
    public long getCurrentLevelBaseXp() {
        return totalXpForLevel(this.currentLevel);
    }

    /** XP total acumulado necessário para alcançar o próximo nível. */
    public long getNextLevelTargetXp() {
        return totalXpForLevel(this.currentLevel + 1);
    }

    /** Quantos pontos de XP o nível atual requer no total para passar de fase. */
    public long getXpRequiredForNextLevel() {
        return getNextLevelTargetXp() - getCurrentLevelBaseXp();
    }

    /** Quantos pontos de XP o jogador já acumulou dentro do nível atual. */
    public long getCurrentLevelProgressXp() {
        return Math.max(0, this.totalXp - getCurrentLevelBaseXp());
    }

    /**
     * Porcentagem da barra de progresso no nível atual (0.00% a 100.00%).
     */
    public BigDecimal getProgressPercentage() {
        long required = getXpRequiredForNextLevel();
        if (required <= 0) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);

        long progress = getCurrentLevelProgressXp();
        return BigDecimal.valueOf(progress)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(required), 2, RoundingMode.HALF_EVEN);
    }
}
