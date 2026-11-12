package com.isaacggr.finance_app.dto.notification;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record NotificationPreferenceUpdateDTO(
        @NotNull(message = "Campo billAlertsEmailEnabled é obrigatório")
        @Schema(description = "Habilita alertas automáticos de contas a vencer por e-mail", example = "true")
        Boolean billAlertsEmailEnabled,

        @NotNull(message = "Campo billAlertsInAppEnabled é obrigatório")
        @Schema(description = "Habilita notificações de contas a vencer e atrasadas no sino do aplicativo", example = "true")
        Boolean billAlertsInAppEnabled,

        @NotNull(message = "Campo billAlertDaysBefore é obrigatório")
        @Min(value = 1, message = "A antecedência mínima do alerta de conta é de 1 dia")
        @Max(value = 30, message = "A antecedência máxima do alerta de conta é de 30 dias")
        @Schema(description = "Quantos dias antes do vencimento enviar o alerta preventivo (1 a 30 dias)", example = "3")
        Integer billAlertDaysBefore,

        @NotNull(message = "Campo monthlyReportEmailEnabled é obrigatório")
        @Schema(description = "Habilita envio automático do relatório financeiro mensal por e-mail", example = "true")
        Boolean monthlyReportEmailEnabled,

        @NotNull(message = "Campo monthlyReportInAppEnabled é obrigatório")
        @Schema(description = "Habilita notificação no sino quando o relatório mensal for gerado", example = "true")
        Boolean monthlyReportInAppEnabled,

        @NotNull(message = "Campo budgetAlertsInAppEnabled é obrigatório")
        @Schema(description = "Habilita notificações no sino ao atingir 80% ou ultrapassar um orçamento mensal", example = "true")
        Boolean budgetAlertsInAppEnabled,

        @NotNull(message = "Campo goalAlertsInAppEnabled é obrigatório")
        @Schema(description = "Habilita notificações no sino quando uma meta financeira atinge 100%", example = "true")
        Boolean goalAlertsInAppEnabled
) {
}
