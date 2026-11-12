package com.isaacggr.finance_app.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record UnreadNotificationsCountDTO(
        @Schema(description = "Quantidade total de notificações não lidas do usuário autenticado", example = "4")
        long unreadCount
) {
}
