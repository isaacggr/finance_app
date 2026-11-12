package com.isaacggr.finance_app.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateDTO(

        @Schema(description = "Nome completo do usuário", example = "Isaac Gregorio")
        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 2, max = 50, message = "O nome deve ter entre 2 e 50 caracteres")
        String name,

        @Schema(description = "E-mail de acesso (deve ser único)", example = "isaac@exemplo.com")
        @Email(message = "Formato de e-mail inválido")
        @NotBlank(message = "O e-mail é obrigatório")
        String email,

        @Schema(description = "Senha segura (mínimo 8 caracteres)", example = "SenhaForte123")
        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 8, max = 64, message = "A senha deve ter entre 8 e 64 caracteres")
        String password
) {}
