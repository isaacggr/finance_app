package com.isaacggr.goldforge.dto.user;

import java.util.UUID;

import com.isaacggr.goldforge.domain.entity.User;

public record UserResponseDTO(
        UUID id,
        String name,
        String email
) {
    public static UserResponseDTO fromEntity(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }
}
