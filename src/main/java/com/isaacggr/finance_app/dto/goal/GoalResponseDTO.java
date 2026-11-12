package com.isaacggr.finance_app.dto.goal;

import com.isaacggr.finance_app.domain.entity.Goal;
import com.isaacggr.finance_app.domain.enums.GoalStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record GoalResponseDTO(
        @Schema(description = "Identificador único da meta", example = "6fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Título da meta financeira", example = "Reserva de Emergência")
        String title,

        @Schema(description = "Descrição detalhada da meta", example = "Guardar 6 meses de despesas fixas")
        String description,

        @Schema(description = "Valor total alvo da meta", example = "15000.00")
        BigDecimal targetAmount,

        @Schema(description = "Valor total já acumulado em aportes", example = "3750.00")
        BigDecimal currentAmount,

        @Schema(description = "Valor restante para atingir o objetivo", example = "11250.00")
        BigDecimal remainingAmount,

        @Schema(description = "Percentual concluído calculado no servidor em escala 2", example = "25.00")
        BigDecimal percentage,

        @Schema(description = "Data alvo opcional da meta", example = "2027-12-31")
        LocalDate deadline,

        @Schema(description = "Indica se esta é a meta principal (favorita) do usuário", example = "true")
        boolean primary,

        @Schema(description = "Status calculado da meta (IN_PROGRESS ou COMPLETED)", example = "IN_PROGRESS")
        GoalStatus status,

        @Schema(description = "Data e hora de criação", example = "2026-11-11T19:50:00")
        LocalDateTime createdAt,

        @Schema(description = "Data e hora da última atualização", example = "2026-11-11T19:55:00")
        LocalDateTime updatedAt
) {
    public static GoalResponseDTO fromEntity(Goal goal) {
        return new GoalResponseDTO(
                goal.getId(),
                goal.getTitle(),
                goal.getDescription(),
                goal.getTargetAmount(),
                goal.getCurrentAmount(),
                goal.getRemainingAmount(),
                goal.getPercentage(),
                goal.getDeadline(),
                goal.isPrimary(),
                goal.getStatus(),
                goal.getCreatedAt(),
                goal.getUpdatedAt()
        );
    }
}
