package com.isaacggr.finance_app.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record NotificationReadAllResponseDTO(
        @Schema(description = "Quantidade de notificações marcadas como lidas nesta operação", example = "5")
        int updatedCount
) {
}
