package com.isaacggr.finance_app.dto;

public record LoginResponseDTO(
        String token,
        String type,
        Long expiresIn
) {
    public static LoginResponseDTO of(String token, Long expiresIn) {
        return new LoginResponseDTO(token, "Bearer", expiresIn);
    }
}

