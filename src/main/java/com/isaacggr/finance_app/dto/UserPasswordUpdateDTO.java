package com.isaacggr.finance_app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Credenciais para alteração de senha do usuário autenticado sem envio de código por e-mail")
public record UserPasswordUpdateDTO(

        @Schema(description = "Senha atual da conta autenticada", example = "SenhaAtual123!")
        @NotBlank(message = "A senha atual é obrigatória")
        String currentPassword,

        @Schema(description = "Nova senha segura (entre 8 e 64 caracteres, diferente da senha atual)", example = "NovaSenhaForte123!")
        @NotBlank(message = "A nova senha é obrigatória")
        @Size(min = 8, max = 64, message = "A nova senha deve ter entre 8 e 64 caracteres")
        String newPassword
) {}
