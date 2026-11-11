package com.isaacggr.finance_app.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.isaacggr.finance_app.domain.entity.Goal;
import com.isaacggr.finance_app.domain.entity.GoalContribution;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GoalContributionResponseDTO(
        @Schema(description = "Identificador único do aporte", example = "a91f7f64-2b43-4c11-941a-758420c45123")
        UUID id,

        @Schema(description = "Identificador da meta vinculada", example = "6fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID goalId,

        @Schema(description = "Valor aportado", example = "500.00")
        BigDecimal amount,

        @Schema(description = "Data do aporte", example = "2026-11-11")
        LocalDate contributionDate,

        @Schema(description = "Observação opcional do aporte", example = "13º salário - 1ª parcela")
        String note,

        @Schema(description = "Data e hora do registro", example = "2026-11-11T19:55:00")
        LocalDateTime createdAt,

        @Schema(description = "Estado atualizado da meta retornado após o novo aporte")
        GoalResponseDTO goal
) {
    public static GoalContributionResponseDTO fromEntity(GoalContribution contribution) {
        return new GoalContributionResponseDTO(
                contribution.getId(),
                contribution.getGoal().getId(),
                contribution.getAmount(),
                contribution.getContributionDate(),
                contribution.getNote(),
                contribution.getCreatedAt(),
                null
        );
    }

    public static GoalContributionResponseDTO fromEntityWithGoal(GoalContribution contribution, Goal goal) {
        return new GoalContributionResponseDTO(
                contribution.getId(),
                goal.getId(),
                contribution.getAmount(),
                contribution.getContributionDate(),
                contribution.getNote(),
                contribution.getCreatedAt(),
                GoalResponseDTO.fromEntity(goal)
        );
    }
}
