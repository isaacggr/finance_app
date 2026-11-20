package com.isaacggr.goldforge.dto.gamification;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Resposta do resgate de recompensa de uma missão (quest)")
public record ClaimQuestResponseDTO(
        @Schema(description = "Identificador da missão resgatada")
        UUID questId,

        @Schema(description = "Pontos de experiência ganhos", example = "50")
        int xpGained,

        @Schema(description = "Novo total de XP acumulado do jogador", example = "350")
        long newTotalXp,

        @Schema(description = "Nível atual do jogador após o resgate", example = "3")
        int currentLevel,

        @Schema(description = "Indica se o resgate causou subida de nível (Level Up!)", example = "true")
        boolean leveledUp,

        @Schema(description = "Mensagem comemorativa do sistema", example = "Parabéns! Você resgatou 50 XP e subiu para o Nível 3!")
        String message
) {}
