package com.isaacggr.finance_app.controller;

import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.dto.common.PageResponseDTO;
import com.isaacggr.finance_app.dto.notification.NotificationResponseDTO;
import com.isaacggr.finance_app.exception.ErrorResponse;
import com.isaacggr.finance_app.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@Tag(name = "Notificações", description = "Central in-app de notificações e contagem de não lidas (sino do aplicativo)")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(
            summary = "Listar notificações",
            description = "Lista notificações do usuário autenticado ordenadas da mais recente para a mais antiga, com filtro opcional por não lidas"
    )
    @ApiResponse(responseCode = "200", description = "Página de notificações retornada com sucesso")
    public ResponseEntity<PageResponseDTO<NotificationResponseDTO>> list(
            @RequestParam(required = false) @Parameter(description = "Quando true, retorna apenas notificações não lidas", example = "true") Boolean unreadOnly,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(notificationService.listNotifications(user.getId(), unreadOnly, pageable));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Contar notificações não lidas", description = "Retorna apenas a contagem total de notificações não lidas para o badge do sino")
    @ApiResponse(responseCode = "200", description = "Contagem retornada com sucesso")
    public ResponseEntity<NotificationResponseDTO.UnreadCount> countUnread(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(notificationService.countUnread(user.getId()));
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "Marcar notificação como lida", description = "Marca uma notificação do usuário autenticado como lida de forma idempotente")
    @ApiResponse(responseCode = "200", description = "Notificação marcada como lida")
    @ApiResponse(responseCode = "404", description = "Notificação não encontrada",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<NotificationResponseDTO> markAsRead(
            @PathVariable UUID notificationId,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(notificationService.markAsRead(notificationId, user.getId()));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Marcar todas as notificações como lidas", description = "Marca todas as notificações não lidas do usuário autenticado como lidas em lote")
    @ApiResponse(responseCode = "200", description = "Notificações atualizadas com sucesso")
    public ResponseEntity<NotificationResponseDTO.ReadAllResult> markAllAsRead(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(notificationService.markAllAsRead(user.getId()));
    }

    @DeleteMapping("/{notificationId}")
    @Operation(summary = "Excluir notificação", description = "Exclui definitivamente uma notificação pertencente ao usuário autenticado")
    @ApiResponse(responseCode = "204", description = "Notificação excluída com sucesso")
    @ApiResponse(responseCode = "404", description = "Notificação não encontrada",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Void> delete(
            @PathVariable UUID notificationId,
            @AuthenticationPrincipal User user
    ) {
        notificationService.delete(notificationId, user.getId());
        return ResponseEntity.noContent().build();
    }
}
