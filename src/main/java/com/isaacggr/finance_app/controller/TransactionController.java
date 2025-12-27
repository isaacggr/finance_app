package com.isaacggr.finance_app.controller;

import com.isaacggr.finance_app.dto.TransactionCreateDTO;
import com.isaacggr.finance_app.dto.TransactionResponseDTO;
import com.isaacggr.finance_app.domain.entity.Transaction;
import com.isaacggr.finance_app.service.TransactionService;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // ✅ Criar transação
    @PostMapping
    public ResponseEntity<TransactionResponseDTO> create(
            @RequestBody @Valid TransactionCreateDTO dto
    ) {
        Transaction transaction = transactionService.create(
                dto.userId(),
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

    // ✅ Buscar transação por ID
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponseDTO> getById(
            @PathVariable UUID transactionId
    ) {
        Transaction transaction = transactionService.findById(transactionId);

        return ResponseEntity.ok(
                TransactionResponseDTO.fromEntity(transaction)
        );
    }

    // ✅ Listar transações do usuário (paginação pronta)
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<TransactionResponseDTO>> listByUser(
            @PathVariable UUID userId,
            Pageable pageable
    ) {
        Page<TransactionResponseDTO> page = transactionService
                .listByUser(userId, pageable)
                .map(TransactionResponseDTO::fromEntity);

        return ResponseEntity.ok(page);
    }

    // ✅ Soft delete
    @DeleteMapping("/{transactionId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID transactionId
    ) {
        transactionService.delete(transactionId);
        return ResponseEntity.noContent().build();
    }
}
