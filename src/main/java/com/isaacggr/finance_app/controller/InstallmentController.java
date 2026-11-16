package com.isaacggr.finance_app.controller;

import com.isaacggr.finance_app.domain.entity.InstallmentGroup;
import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.dto.common.PageResponseDTO;
import com.isaacggr.finance_app.dto.installment.InstallmentGroupResponseDTO;
import com.isaacggr.finance_app.dto.installment.InstallmentPurchaseCreateDTO;
import com.isaacggr.finance_app.exception.ErrorResponse;
import com.isaacggr.finance_app.service.InstallmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/installments")
@Tag(name = "Compras Parceladas", description = "Endpoints para gerenciamento de compras parceladas no cartão de crédito, geração de parcelas e controle de faturas futuras")
public class InstallmentController {

    private final InstallmentService installmentService;

    public InstallmentController(InstallmentService installmentService) {
        this.installmentService = installmentService;
    }

    @PostMapping
    @Operation(
            summary = "Cadastrar compra parcelada",
            description = "Cria uma compra parcelada, gerando automaticamente N despesas distribuídas mês a mês com cálculo exato de centavos (Remainder Distribution)"
    )
    @ApiResponse(responseCode = "201", description = "Compra parcelada cadastrada com sucesso")
    @ApiResponse(responseCode = "404", description = "Usuário autenticado não encontrado ou inativo",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Conflito nas regras de parcelamento",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<InstallmentGroupResponseDTO> create(
            @RequestBody @Valid InstallmentPurchaseCreateDTO dto,
            @AuthenticationPrincipal User user
    ) {
        InstallmentGroup group = installmentService.create(user, dto);
        InstallmentGroupResponseDTO response = InstallmentGroupResponseDTO.fromEntity(group);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(group.getId())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @Operation(
            summary = "Listar compras parceladas",
            description = "Lista todas as compras parceladas do usuário autenticado com suporte a paginação"
    )
    @ApiResponse(responseCode = "200", description = "Página de compras parceladas retornada com sucesso")
    public ResponseEntity<PageResponseDTO<InstallmentGroupResponseDTO>> list(
            @AuthenticationPrincipal User user,
            @ParameterObject @PageableDefault(size = 15, sort = "startDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<InstallmentGroupResponseDTO> page = installmentService
                .listByUser(user.getId(), pageable)
                .map(InstallmentGroupResponseDTO::fromEntity);

        return ResponseEntity.ok(PageResponseDTO.from(page));
    }

    @GetMapping("/{installmentGroupId}")
    @Operation(
            summary = "Buscar compra parcelada por ID",
            description = "Retorna os detalhes de uma compra parcelada específica e todas as suas parcelas associadas"
    )
    @ApiResponse(responseCode = "200", description = "Compra parcelada encontrada com sucesso")
    @ApiResponse(responseCode = "404", description = "Compra parcelada não encontrada para o usuário autenticado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<InstallmentGroupResponseDTO> getById(
            @PathVariable @Parameter(description = "Identificador único (UUID) do grupo de parcelamento", example = "d94b15bb-09c3-4d43-9824-76ec5a7cf11e") UUID installmentGroupId,
            @AuthenticationPrincipal User user
    ) {
        InstallmentGroup group = installmentService.findById(installmentGroupId, user.getId());
        return ResponseEntity.ok(InstallmentGroupResponseDTO.fromEntity(group));
    }

    @DeleteMapping("/{installmentGroupId}")
    @Operation(
            summary = "Excluir compra parcelada",
            description = "Exclui em definitivo a compra parcelada e todas as transações de parcelas vinculadas a ela (Hard Delete em cascata)"
    )
    @ApiResponse(responseCode = "204", description = "Compra parcelada e parcelas excluídas com sucesso")
    @ApiResponse(responseCode = "404", description = "Compra parcelada não encontrada para o usuário autenticado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Void> delete(
            @PathVariable @Parameter(description = "Identificador único (UUID) do grupo de parcelamento", example = "d94b15bb-09c3-4d43-9824-76ec5a7cf11e") UUID installmentGroupId,
            @AuthenticationPrincipal User user
    ) {
        installmentService.delete(installmentGroupId, user.getId());
        return ResponseEntity.noContent().build();
    }
}
