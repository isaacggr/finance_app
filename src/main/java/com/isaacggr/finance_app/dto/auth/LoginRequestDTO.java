package com.isaacggr.finance_app.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
        @Schema(description = "E-mail cadastrado", example = "isaac@exemplo.com")
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        String email,

        @Schema(description = "Senha da conta", example = "SenhaForte123")
        @NotBlank(message = "A senha é obrigatória")
        String password
) {}

