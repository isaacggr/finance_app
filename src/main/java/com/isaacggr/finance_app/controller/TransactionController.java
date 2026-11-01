package com.isaacggr.finance_app.controller;

import com.isaacggr.finance_app.domain.entity.Transaction;
import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.dto.PageResponseDTO;
import com.isaacggr.finance_app.dto.TransactionCreateDTO;
import com.isaacggr.finance_app.dto.TransactionResponseDTO;
import com.isaacggr.finance_app.dto.TransactionUpdateDTO;
import com.isaacggr.finance_app.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
    @Operation(summary = "Criar transação", description = "Cria uma nova receita ou despesa associada ao usuário autenticado")
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

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(TransactionResponseDTO.fromEntity(transaction));
    }

    // ✅ Listar transações do usuário autenticado (paginadas)
    @GetMapping
    @Operation(summary = "Listar transações", description = "Lista as transações do usuário autenticado com paginação")
    public ResponseEntity<PageResponseDTO<TransactionResponseDTO>> list(
            @AuthenticationPrincipal User user,
            Pageable pageable
    ) {
        Page<TransactionResponseDTO> page = transactionService
                .listByUser(user.getId(), pageable)
                .map(TransactionResponseDTO::fromEntity);

        return ResponseEntity.ok(PageResponseDTO.from(page));
    }

    // ✅ Buscar transação por ID (apenas se pertencer ao usuário autenticado)
    @GetMapping("/{transactionId}")
    @Operation(summary = "Buscar transação por ID", description = "Busca uma transação específica pertencente ao usuário autenticado")
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

    // ✅ Soft delete (apenas se pertencer ao usuário autenticado)
    @DeleteMapping("/{transactionId}")
    @Operation(summary = "Excluir transação", description = "Exclui logicamente uma transação pertencente ao usuário autenticado")
    public ResponseEntity<Void> delete(
            @PathVariable @Parameter(description = "Identificador único (UUID) da transação", example = "05498412-beb1-4060-ae2a-74152f144619") UUID transactionId,
            @AuthenticationPrincipal User user
    ) {
        transactionService.delete(transactionId, user.getId());
        return ResponseEntity.noContent().build();
    }
}
