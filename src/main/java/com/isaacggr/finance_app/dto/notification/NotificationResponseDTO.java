package com.isaacggr.finance_app.dto.notification;

import com.isaacggr.finance_app.domain.entity.Notification;
import com.isaacggr.finance_app.domain.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponseDTO(
        @Schema(description = "Identificador único da notificação")
        UUID id,

        @Schema(description = "Categoria semântica do evento", example = "BILL_DUE_TODAY")
        NotificationType type,

        @Schema(description = "Título curto exibido no sino", example = "Conta vence hoje: Internet Fibra")
        String title,

        @Schema(description = "Descrição detalhada da notificação")
        String message,

        @Schema(description = "Tipo do recurso relacionado para deep-link na interface (BILL, BUDGET, GOAL, REPORT)", example = "BILL")
        String referenceType,

        @Schema(description = "Identificador do recurso relacionado, quando aplicável")
        UUID referenceId,

        @Schema(description = "Indica se a notificação já foi lida", example = "false")
        boolean read,

        @Schema(description = "Data e hora em que a notificação foi marcada como lida")
        LocalDateTime readAt,

        @Schema(description = "Data e hora de geração da notificação")
        LocalDateTime createdAt
) {
    public static NotificationResponseDTO fromEntity(Notification notification) {
        return new NotificationResponseDTO(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getReferenceType(),
                notification.getReferenceId(),
                notification.isRead(),
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }

    @Schema(name = "UnreadNotificationsCount", description = "Contagem total de notificações não lidas")
    public record UnreadCount(
            @Schema(description = "Quantidade total de notificações não lidas do usuário autenticado", example = "4")
            long unreadCount
    ) {
    }

    @Schema(name = "NotificationReadAllResponse", description = "Resultado da marcação em lote de notificações como lidas")
    public record ReadAllResult(
            @Schema(description = "Quantidade de notificações marcadas como lidas nesta operação", example = "5")
            int updatedCount
    ) {
    }
}
