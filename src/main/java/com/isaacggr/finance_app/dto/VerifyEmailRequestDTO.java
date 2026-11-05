package com.isaacggr.finance_app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyEmailRequestDTO(

        @Schema(description = "E-mail cadastrado do usuário", example = "usuario@example.com")
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        String email,

        @Schema(description = "Código de ativação de 6 dígitos recebido por e-mail", example = "123456")
        @NotBlank(message = "O código de verificação é obrigatório")
        @Pattern(regexp = "^\\d{6}$", message = "O código deve conter exatamente 6 dígitos numéricos")
        String code
) {}

