package com.isaacggr.finance_app.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponseDTO(
        String token,
        String type,
        Long expiresIn,
        @Schema(description = "Presente como true quando uma conta excluída dentro do prazo de 30 dias é reativada automaticamente neste login", example = "true")
        Boolean reactivated
) {
    public LoginResponseDTO(String token, String type, Long expiresIn) {
        this(token, type, expiresIn, null);
    }

    public static LoginResponseDTO of(String token, Long expiresIn) {
        return new LoginResponseDTO(token, "Bearer", expiresIn, null);
    }

    public static LoginResponseDTO reactivated(String token, Long expiresIn) {
        return new LoginResponseDTO(token, "Bearer", expiresIn, true);
    }
}
