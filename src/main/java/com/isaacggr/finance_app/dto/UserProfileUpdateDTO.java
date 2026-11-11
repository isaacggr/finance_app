package com.isaacggr.finance_app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados atualizáveis do perfil do usuário autenticado")
public record UserProfileUpdateDTO(

        @Schema(description = "Nome completo atualizado do usuário", example = "Isaac Gregório")
        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 2, max = 50, message = "O nome deve ter entre 2 e 50 caracteres")
        String name
) {}
