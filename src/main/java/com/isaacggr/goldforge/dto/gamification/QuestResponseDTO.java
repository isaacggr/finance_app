package com.isaacggr.goldforge.dto.gamification;

import com.isaacggr.goldforge.domain.entity.UserQuest;
import com.isaacggr.goldforge.domain.enums.QuestCategory;
import com.isaacggr.goldforge.domain.enums.QuestStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Dados da missão (quest) financeira do FinanGame")
public record QuestResponseDTO(
        @Schema(description = "Identificador único da missão do usuário")
        UUID id,

        @Schema(description = "Código identificador da regra da missão", example = "NO_SPEND_DAY")
        String questCode,

        @Schema(description = "Categoria da missão (DAILY, WEEKLY, MILESTONE)", example = "DAILY")
        QuestCategory category,

        @Schema(description = "Título descritivo da missão", example = "Dia de Gasto Zero")
        String title,

        @Schema(description = "Descrição detalhada do objetivo financeiro", example = "Passe o dia sem despesas registradas e proteja seu dinheiro.")
        String description,

        @Schema(description = "Recompensa em pontos de experiência (XP)", example = "30")
        int xpReward,

        @Schema(description = "Progresso atual acumulado", example = "1")
        int currentProgress,

        @Schema(description = "Meta necessária para concluir a missão", example = "1")
        int targetProgress,

        @Schema(description = "Status da missão (IN_PROGRESS, COMPLETED, CLAIMED)", example = "COMPLETED")
        QuestStatus status,

        @Schema(description = "Chave do período da missão", example = "2026-11-15")
        String periodKey,

        @Schema(description = "Data/hora de expiração da missão")
        LocalDateTime expiresAt,

        @Schema(description = "Data/hora em que a missão foi concluída")
        LocalDateTime completedAt,

        @Schema(description = "Data/hora em que a recompensa foi resgatada")
        LocalDateTime claimedAt
) {
    public static QuestResponseDTO fromEntity(UserQuest quest) {
        return new QuestResponseDTO(
                quest.getId(),
                quest.getQuestCode(),
                quest.getCategory(),
                quest.getTitle(),
                quest.getDescription(),
                quest.getXpReward(),
                quest.getCurrentProgress(),
                quest.getTargetProgress(),
                quest.getStatus(),
                quest.getPeriodKey(),
                quest.getExpiresAt(),
                quest.getCompletedAt(),
                quest.getClaimedAt()
        );
    }
}
