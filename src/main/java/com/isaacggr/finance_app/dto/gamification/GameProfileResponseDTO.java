package com.isaacggr.finance_app.dto.gamification;

import com.isaacggr.finance_app.domain.entity.UserGameProfile;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Perfil de progressão e nível RPG do jogador no FinanGame")
public record GameProfileResponseDTO(
        @Schema(description = "Nível atual do jogador", example = "3")
        int level,

        @Schema(description = "Total acumulado de pontos de experiência", example = "450")
        long totalXp,

        @Schema(description = "XP acumulado dentro do nível atual", example = "150")
        long currentLevelProgressXp,

        @Schema(description = "XP total necessário para subir do nível atual para o próximo", example = "300")
        long xpRequiredForNextLevel,

        @Schema(description = "Porcentagem de preenchimento da barra de XP (0.00% a 100.00%)", example = "50.00")
        BigDecimal progressPercentage,

        @Schema(description = "Sequência de dias consecutivos de consciência financeira (streak)", example = "5")
        int streakDays,

        @Schema(description = "Data do último check-in diário realizado", example = "2026-11-15")
        LocalDate lastCheckinDate,

        @Schema(description = "Indica se o jogador ainda pode realizar o check-in diário hoje", example = "true")
        boolean checkinAvailableToday
) {
    public static GameProfileResponseDTO fromEntity(UserGameProfile profile, LocalDate today) {
        boolean available = profile.getLastCheckinDate() == null || !profile.getLastCheckinDate().equals(today);
        return new GameProfileResponseDTO(
                profile.getCurrentLevel(),
                profile.getTotalXp(),
                profile.getCurrentLevelProgressXp(),
                profile.getXpRequiredForNextLevel(),
                profile.getProgressPercentage(),
                profile.getStreakDays(),
                profile.getLastCheckinDate(),
                available
        );
    }
}
