package com.isaacggr.finance_app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateDTO(

        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 2, max = 50, message = "O nome deve ter entre 2 e 50 caracteres")
        String name,

        @Email(message = "Formato de e-mail inválido")
        @NotBlank(message = "O e-mail é obrigatório")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 8, max = 64, message = "A senha deve ter entre 8 e 64 caracteres")
        String password
) {}
