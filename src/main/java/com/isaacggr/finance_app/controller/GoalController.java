package com.isaacggr.finance_app.controller;

import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.dto.GoalContributionCreateDTO;
import com.isaacggr.finance_app.dto.GoalContributionResponseDTO;
import com.isaacggr.finance_app.dto.GoalCreateDTO;
import com.isaacggr.finance_app.dto.GoalResponseDTO;
import com.isaacggr.finance_app.dto.GoalUpdateDTO;
import com.isaacggr.finance_app.exception.ErrorResponse;
import com.isaacggr.finance_app.service.GoalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/goals")
@Tag(name = "Metas Financeiras", description = "Endpoints para criação de metas de economia, destaque principal no dashboard e histórico de aportes")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @PostMapping
    @Operation(summary = "Criar meta financeira", description = "Cria uma nova meta para o usuário autenticado. Por padrão, torna-se a meta principal (primary=true) e aceita saldo inicial opcional")
    @ApiResponse(responseCode = "201", description = "Meta financeira criada com sucesso")
    public ResponseEntity<GoalResponseDTO> create(
            @RequestBody @Valid GoalCreateDTO dto,
            @AuthenticationPrincipal User user
    ) {
        GoalResponseDTO response = goalService.create(user, dto);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar metas financeiras", description = "Lista todas as metas do usuário autenticado ordenadas pela meta principal primeiro e depois pelas mais recentes")
    public ResponseEntity<List<GoalResponseDTO>> list(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(goalService.listAll(user.getId()));
    }

    @GetMapping("/primary")
    @Operation(summary = "Consultar meta principal", description = "Retorna a meta atualmente destacada (primary=true) para exibição no card principal do dashboard, ou 204 caso não existam metas")
    @ApiResponse(responseCode = "200", description = "Meta principal retornada com sucesso")
    @ApiResponse(responseCode = "204", description = "Usuário ainda não possui metas cadastradas")
    public ResponseEntity<GoalResponseDTO> getPrimary(
            @AuthenticationPrincipal User user
    ) {
        return goalService.findPrimary(user.getId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{goalId}")
    @Operation(summary = "Buscar meta por ID", description = "Retorna os detalhes de uma meta específica pertencente ao usuário autenticado")
    @ApiResponse(responseCode = "200", description = "Meta encontrada")
    @ApiResponse(responseCode = "404", description = "Meta financeira não encontrada para o usuário autenticado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<GoalResponseDTO> getById(
            @PathVariable @Parameter(description = "Identificador único (UUID) da meta", example = "6fa85f64-5717-4562-b3fc-2c963f66afa6") UUID goalId,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(goalService.findById(goalId, user.getId()));
    }

    @PutMapping("/{goalId}")
    @Operation(summary = "Atualizar meta financeira", description = "Atualiza título, descrição, valor alvo, data limite e opcionalmente o destaque principal da meta")
    @ApiResponse(responseCode = "200", description = "Meta atualizada com sucesso")
    @ApiResponse(responseCode = "404", description = "Meta financeira não encontrada para o usuário autenticado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<GoalResponseDTO> update(
            @PathVariable @Parameter(description = "Identificador único (UUID) da meta", example = "6fa85f64-5717-4562-b3fc-2c963f66afa6") UUID goalId,
            @RequestBody @Valid GoalUpdateDTO dto,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(goalService.update(goalId, user.getId(), dto));
    }

    @PatchMapping("/{goalId}/primary")
    @Operation(summary = "Favoritar meta como principal", description = "Define a meta escolhida como principal (primary=true), desmarcando automaticamente a favorita anterior")
    @ApiResponse(responseCode = "200", description = "Meta definida como principal com sucesso")
    @ApiResponse(responseCode = "404", description = "Meta financeira não encontrada para o usuário autenticado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<GoalResponseDTO> setPrimary(
            @PathVariable @Parameter(description = "Identificador único (UUID) da meta", example = "6fa85f64-5717-4562-b3fc-2c963f66afa6") UUID goalId,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(goalService.setAsPrimary(goalId, user.getId()));
    }

    @DeleteMapping("/{goalId}")
    @Operation(summary = "Excluir meta financeira", description = "Exclui fisicamente a meta e seus aportes (Hard Delete). Caso fosse a meta principal, promove automaticamente a mais recente restante")
    @ApiResponse(responseCode = "204", description = "Meta excluída com sucesso")
    @ApiResponse(responseCode = "404", description = "Meta financeira não encontrada para o usuário autenticado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Void> delete(
            @PathVariable @Parameter(description = "Identificador único (UUID) da meta", example = "6fa85f64-5717-4562-b3fc-2c963f66afa6") UUID goalId,
            @AuthenticationPrincipal User user
    ) {
        goalService.delete(goalId, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{goalId}/contributions")
    @Operation(summary = "Registrar aporte na meta", description = "Adiciona um aporte financeiro à meta, recalculando saldo acumulado, percentual e status no servidor")
    @ApiResponse(responseCode = "201", description = "Aporte registrado com sucesso")
    @ApiResponse(responseCode = "404", description = "Meta financeira não encontrada para o usuário autenticado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<GoalContributionResponseDTO> addContribution(
            @PathVariable @Parameter(description = "Identificador único (UUID) da meta", example = "6fa85f64-5717-4562-b3fc-2c963f66afa6") UUID goalId,
            @RequestBody @Valid GoalContributionCreateDTO dto,
            @AuthenticationPrincipal User user
    ) {
        GoalContributionResponseDTO response = goalService.addContribution(goalId, user.getId(), dto);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{contributionId}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{goalId}/contributions")
    @Operation(summary = "Listar aportes da meta", description = "Retorna o histórico de aportes de uma meta pertencente ao usuário autenticado")
    @ApiResponse(responseCode = "200", description = "Lista de aportes retornada com sucesso")
    @ApiResponse(responseCode = "404", description = "Meta financeira não encontrada para o usuário autenticado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<List<GoalContributionResponseDTO>> listContributions(
            @PathVariable @Parameter(description = "Identificador único (UUID) da meta", example = "6fa85f64-5717-4562-b3fc-2c963f66afa6") UUID goalId,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(goalService.listContributions(goalId, user.getId()));
    }

    @DeleteMapping("/{goalId}/contributions/{contributionId}")
    @Operation(summary = "Estornar aporte da meta", description = "Remove fisicamente o aporte informado e subtrai seu valor do saldo acumulado da meta")
    @ApiResponse(responseCode = "204", description = "Aporte estornado com sucesso")
    @ApiResponse(responseCode = "404", description = "Meta ou aporte não encontrado para o usuário autenticado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Void> deleteContribution(
            @PathVariable @Parameter(description = "Identificador único (UUID) da meta", example = "6fa85f64-5717-4562-b3fc-2c963f66afa6") UUID goalId,
            @PathVariable @Parameter(description = "Identificador único (UUID) do aporte", example = "a91f7f64-2b43-4c11-941a-758420c45123") UUID contributionId,
            @AuthenticationPrincipal User user
    ) {
        goalService.deleteContribution(goalId, contributionId, user.getId());
        return ResponseEntity.noContent().build();
    }
}
