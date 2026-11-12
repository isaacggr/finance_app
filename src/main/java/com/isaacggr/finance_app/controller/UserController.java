package com.isaacggr.finance_app.controller;

import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.dto.NotificationPreferenceResponseDTO;
import com.isaacggr.finance_app.dto.NotificationPreferenceUpdateDTO;
import com.isaacggr.finance_app.dto.UserCreateDTO;
import com.isaacggr.finance_app.dto.UserPasswordUpdateDTO;
import com.isaacggr.finance_app.dto.UserProfileUpdateDTO;
import com.isaacggr.finance_app.dto.UserResponseDTO;
import com.isaacggr.finance_app.exception.ErrorResponse;
import com.isaacggr.finance_app.service.NotificationPreferenceService;
import com.isaacggr.finance_app.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/users")
@Tag(name = "Usuários", description = "Endpoints para cadastro e gerenciamento de perfil de usuários")
public class UserController {

    private final UserService service;
    private final NotificationPreferenceService preferenceService;

    public UserController(UserService service, NotificationPreferenceService preferenceService) {
        this.service = service;
        this.preferenceService = preferenceService;
    }

    // ✅ Criar usuário (Rota pública de cadastro)
    @PostMapping
    @Operation(summary = "Cadastrar usuário", description = "Cria um novo usuário na aplicação com senha criptografada via BCrypt. Rota pública.")
    @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso")
    @ApiResponse(responseCode = "409", description = "E-mail já cadastrado ou em período de retenção de 30 dias",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<UserResponseDTO> create(
            @RequestBody @Valid UserCreateDTO dto
    ) {
        UserResponseDTO response = service.create(dto);
        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/users/me")
                .build()
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    // ✅ Obter perfil do usuário autenticado (Zero IDOR)
    @GetMapping("/me")
    @Operation(summary = "Obter perfil autenticado", description = "Retorna os dados cadastrais do próprio usuário autenticado. Essencial para o carregamento de sessão no front-end.")
    @ApiResponse(responseCode = "200", description = "Perfil retornado com sucesso")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<UserResponseDTO> getProfile(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(service.findById(user.getId()));
    }

    // ✅ Atualizar nome do perfil autenticado (Zero IDOR)
    @PatchMapping("/me")
    @Operation(summary = "Atualizar perfil autenticado", description = "Atualiza os dados editáveis do perfil (nome) do usuário autenticado sem necessidade de verificação por e-mail")
    @ApiResponse(responseCode = "200", description = "Perfil atualizado com sucesso")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<UserResponseDTO> updateProfile(
            @RequestBody @Valid UserProfileUpdateDTO dto,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(service.updateProfile(user.getId(), dto));
    }

    // ✅ Alterar senha do usuário autenticado (exige senha atual, sem código por e-mail)
    @PatchMapping("/me/password")
    @Operation(summary = "Alterar senha do perfil autenticado", description = "Altera a senha da conta autenticada validando a senha atual via BCrypt, sem envio de código por e-mail")
    @ApiResponse(responseCode = "200", description = "Senha alterada com sucesso")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Map<String, String>> changePassword(
            @RequestBody @Valid UserPasswordUpdateDTO dto,
            @AuthenticationPrincipal User user
    ) {
        service.changePassword(user.getId(), dto);
        return ResponseEntity.ok(Map.of("message", "Senha alterada com sucesso."));
    }

    @GetMapping("/me/notification-preferences")
    @Operation(
            summary = "Obter preferências de notificação",
            description = "Retorna as preferências de alertas por e-mail e notificações no aplicativo do usuário autenticado"
    )
    @ApiResponse(responseCode = "200", description = "Preferências retornadas com sucesso")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<NotificationPreferenceResponseDTO> getNotificationPreferences(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(preferenceService.getPreferences(user.getId()));
    }

    @PutMapping("/me/notification-preferences")
    @Operation(
            summary = "Atualizar preferências de notificação",
            description = "Atualiza as preferências de alertas de contas, relatórios mensais, orçamentos e metas do usuário autenticado"
    )
    @ApiResponse(responseCode = "200", description = "Preferências atualizadas com sucesso")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<NotificationPreferenceResponseDTO> updateNotificationPreferences(
            @RequestBody @Valid NotificationPreferenceUpdateDTO dto,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(preferenceService.updatePreferences(user.getId(), dto));
    }

    // ✅ Excluir própria conta (Zero IDOR)
    @DeleteMapping("/me")
    @Operation(summary = "Excluir própria conta", description = "Realiza a exclusão lógica (soft delete) da conta do próprio usuário autenticado com período de retenção de 30 dias")
    @ApiResponse(responseCode = "204", description = "Conta desativada com sucesso")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Usuário já se encontra desativado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal User user
    ) {
        service.delete(user.getId());
        return ResponseEntity.noContent().build();
    }
}
