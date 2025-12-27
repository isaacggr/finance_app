package com.isaacggr.finance_app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateDTO(

        @NotBlank
        @Size(min = 2, max = 50)
        String name,

        @Email
        @NotBlank
        String email,

        @NotBlank
        @Size(min = 8, max = 64)
        String password
) {}
