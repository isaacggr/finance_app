package com.isaacggr.finance_app.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequestDTO(

        @Schema(description = "E-mail cadastrado na aplicação", example = "usuario@example.com")
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        String email,

        @Schema(description = "Código de 6 dígitos recebido por e-mail", example = "482910")
        @NotBlank(message = "O código de verificação é obrigatório")
        @Pattern(regexp = "^\\d{6}$", message = "O código deve conter exatamente 6 dígitos numéricos")
        String code,

        @Schema(description = "Nova senha segura (mínimo 8 caracteres)", example = "NovaSenhaForte123!")
        @NotBlank(message = "A nova senha é obrigatória")
        @Size(min = 8, max = 64, message = "A nova senha deve ter entre 8 e 64 caracteres")
        String newPassword
) {}

