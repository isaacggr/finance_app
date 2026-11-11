package com.isaacggr.finance_app.controller;

import com.isaacggr.finance_app.domain.entity.Transaction;
import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.domain.enums.TransactionType;
import com.isaacggr.finance_app.dto.PageResponseDTO;
import com.isaacggr.finance_app.dto.TransactionCreateDTO;
import com.isaacggr.finance_app.dto.TransactionResponseDTO;
import com.isaacggr.finance_app.dto.TransactionUpdateDTO;
import com.isaacggr.finance_app.exception.ErrorResponse;
import com.isaacggr.finance_app.service.TransactionService;
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
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/transactions")
@Tag(name = "Transações", description = "Endpoints para gerenciamento de receitas e despesas do usuário autenticado")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // ✅ Criar transação (associada automaticamente ao usuário logado)
    @PostMapping
    @Operation(summary = "Criar transação", description = "Cria uma nova receita ou despesa associada ao usuário autenticado com categoria flexível")
    @ApiResponse(responseCode = "201", description = "Transação criada com sucesso")
    @ApiResponse(responseCode = "404", description = "Usuário autenticado não encontrado ou inativo",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Conflito nas regras da transação",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<TransactionResponseDTO> create(
            @RequestBody @Valid TransactionCreateDTO dto,
            @AuthenticationPrincipal User user
    ) {
        Transaction transaction = transactionService.create(
                user,
                dto.amount(),
                dto.type(),
                dto.category(),
                dto.date(),
                dto.description()
        );

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(transaction.getId())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(TransactionResponseDTO.fromEntity(transaction));
    }

    // ✅ Listar transações do usuário autenticado (paginadas e com filtros opcionais)
    @GetMapping
    @Operation(summary = "Listar transações", description = "Lista as transações do usuário autenticado com paginação e filtros opcionais por período, categoria e tipo")
    @ApiResponse(responseCode = "200", description = "Página de transações retornada com sucesso")
    @ApiResponse(responseCode = "409", description = "Intervalo de datas inválido (startDate posterior a endDate)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<PageResponseDTO<TransactionResponseDTO>> list(
            @RequestParam(required = false) @Parameter(description = "Data inicial (AAAA-MM-DD)", example = "2026-11-01") LocalDate startDate,
            @RequestParam(required = false) @Parameter(description = "Data final (AAAA-MM-DD)", example = "2026-11-30") LocalDate endDate,
            @RequestParam(required = false) @Parameter(description = "Categoria da transação (ex: ALIMENTACAO, PETS)", example = "ALIMENTACAO") String category,
            @RequestParam(required = false) @Parameter(description = "Tipo da transação (INCOME ou EXPENSE)", example = "EXPENSE") TransactionType type,
            @AuthenticationPrincipal User user,
            @ParameterObject @PageableDefault(size = 15, sort = "date", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<TransactionResponseDTO> page = transactionService
                .listByUser(user.getId(), startDate, endDate, category, type, pageable)
                .map(TransactionResponseDTO::fromEntity);

        return ResponseEntity.ok(PageResponseDTO.from(page));
    }

    // ✅ Buscar transação por ID (apenas se pertencer ao usuário autenticado)
    @GetMapping("/{transactionId}")
    @Operation(summary = "Buscar transação por ID", description = "Busca uma transação específica pertencente ao usuário autenticado")
    @ApiResponse(responseCode = "200", description = "Transação encontrada")
    @ApiResponse(responseCode = "404", description = "Transação não encontrada para o usuário autenticado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<TransactionResponseDTO> getById(
            @PathVariable @Parameter(description = "Identificador único (UUID) da transação", example = "05498412-beb1-4060-ae2a-74152f144619") UUID transactionId,
            @AuthenticationPrincipal User user
    ) {
        Transaction transaction = transactionService.findById(transactionId, user.getId());

        return ResponseEntity.ok(
                TransactionResponseDTO.fromEntity(transaction)
        );
    }

    // ✅ Atualizar transação existente do usuário autenticado
    @PutMapping("/{transactionId}")
    @Operation(summary = "Atualizar transação", description = "Atualiza completamente os dados de uma transação existente do usuário autenticado")
    @ApiResponse(responseCode = "200", description = "Transação atualizada com sucesso")
    @ApiResponse(responseCode = "404", description = "Transação não encontrada para o usuário autenticado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Conflito nas regras da transação",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<TransactionResponseDTO> update(
            @PathVariable @Parameter(description = "Identificador único (UUID) da transação", example = "05498412-beb1-4060-ae2a-74152f144619") UUID transactionId,
            @RequestBody @Valid TransactionUpdateDTO dto,
            @AuthenticationPrincipal User user
    ) {
        Transaction updated = transactionService.update(
                transactionId,
                user.getId(),
                dto.amount(),
                dto.type(),
                dto.category(),
                dto.date(),
                dto.description()
        );

        return ResponseEntity.ok(TransactionResponseDTO.fromEntity(updated));
    }

    // ✅ Hard delete definitivo (apenas se pertencer ao usuário autenticado)
    @DeleteMapping("/{transactionId}")
    @Operation(summary = "Excluir transação", description = "Exclui fisicamente e em definitivo uma transação pertencente ao usuário autenticado (Hard Delete)")
    @ApiResponse(responseCode = "204", description = "Transação excluída com sucesso")
    @ApiResponse(responseCode = "404", description = "Transação não encontrada para o usuário autenticado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Void> delete(
            @PathVariable @Parameter(description = "Identificador único (UUID) da transação", example = "05498412-beb1-4060-ae2a-74152f144619") UUID transactionId,
            @AuthenticationPrincipal User user
    ) {
        transactionService.delete(transactionId, user.getId());
        return ResponseEntity.noContent().build();
    }
}
