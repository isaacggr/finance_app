package com.isaacggr.finance_app.dto.notification;

import com.isaacggr.finance_app.domain.entity.UserNotificationPreference;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record NotificationPreferenceResponseDTO(
        @Schema(description = "Habilita alertas automáticos de contas a vencer por e-mail", example = "true")
        boolean billAlertsEmailEnabled,

        @Schema(description = "Habilita notificações de contas a vencer e atrasadas no sino do aplicativo", example = "true")
        boolean billAlertsInAppEnabled,

        @Schema(description = "Quantos dias antes do vencimento enviar o alerta preventivo (1 a 30 dias)", example = "3")
        int billAlertDaysBefore,

        @Schema(description = "Habilita envio automático do relatório financeiro mensal por e-mail", example = "true")
        boolean monthlyReportEmailEnabled,

        @Schema(description = "Habilita notificação no sino do aplicativo quando o relatório mensal for gerado", example = "true")
        boolean monthlyReportInAppEnabled,

        @Schema(description = "Habilita notificações no sino ao atingir 80% ou ultrapassar um orçamento mensal", example = "true")
        boolean budgetAlertsInAppEnabled,

        @Schema(description = "Habilita notificações no sino quando uma meta financeira atinge 100%", example = "true")
        boolean goalAlertsInAppEnabled,

        @Schema(description = "Data e hora da última atualização das preferências")
        LocalDateTime updatedAt
) {
    public static NotificationPreferenceResponseDTO fromEntity(UserNotificationPreference entity) {
        return new NotificationPreferenceResponseDTO(
                entity.isBillAlertsEmailEnabled(),
                entity.isBillAlertsInAppEnabled(),
                entity.getBillAlertDaysBefore(),
                entity.isMonthlyReportEmailEnabled(),
                entity.isMonthlyReportInAppEnabled(),
                entity.isBudgetAlertsInAppEnabled(),
                entity.isGoalAlertsInAppEnabled(),
                entity.getUpdatedAt()
        );
    }

    public static NotificationPreferenceResponseDTO defaultValues() {
        return new NotificationPreferenceResponseDTO(
                true,
                true,
                UserNotificationPreference.DEFAULT_BILL_ALERT_DAYS_BEFORE,
                true,
                true,
                true,
                true,
                null
        );
    }
}
