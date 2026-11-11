package com.isaacggr.finance_app.controller;

import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.dto.BudgetCreateDTO;
import com.isaacggr.finance_app.dto.BudgetProgressDTO;
import com.isaacggr.finance_app.dto.BudgetResponseDTO;
import com.isaacggr.finance_app.dto.BudgetUpdateDTO;
import com.isaacggr.finance_app.service.BudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequestMapping("/budgets")
@Tag(name = "Orçamentos", description = "Endpoints para gerenciamento de metas e tetos de gastos mensais")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    // ✅ Criar orçamento/meta
    @PostMapping
    @Operation(summary = "Definir orçamento", description = "Cria uma meta de teto máximo de gastos para uma categoria em um determinado mês e ano")
    public ResponseEntity<BudgetResponseDTO> create(
            @RequestBody @Valid BudgetCreateDTO dto,
            @AuthenticationPrincipal User user
    ) {
        BudgetResponseDTO response = budgetService.create(user.getId(), user, dto);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    // ✅ Listar orçamentos do período
    @GetMapping
    @Operation(summary = "Listar orçamentos", description = "Retorna os orçamentos cadastrados para o mês e ano informados (ou mês corrente caso omitidos)")
    public ResponseEntity<List<BudgetResponseDTO>> list(
            @RequestParam(required = false) @Parameter(description = "Mês de referência (1-12)", example = "11") Integer month,
            @RequestParam(required = false) @Parameter(description = "Ano de referência", example = "2026") Integer year,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(budgetService.listByPeriod(user.getId(), month, year));
    }

    // ✅ Consultar progresso/consumo dos orçamentos (alimentação para barras no front-end)
    @GetMapping("/progress")
    @Operation(summary = "Consultar progresso dos orçamentos", description = "Compara o teto planejado com os gastos reais de cada categoria, calculando percentual consumido e valor restante")
    public ResponseEntity<List<BudgetProgressDTO>> getProgress(
            @RequestParam(required = false) @Parameter(description = "Mês de referência (1-12)", example = "11") Integer month,
            @RequestParam(required = false) @Parameter(description = "Ano de referência", example = "2026") Integer year,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(budgetService.getProgress(user.getId(), month, year));
    }

    // ✅ Atualizar orçamento existente do usuário autenticado (Zero IDOR)
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar orçamento", description = "Atualiza o valor teto e opcionalmente categoria, mês ou ano de um orçamento do usuário autenticado")
    public ResponseEntity<BudgetResponseDTO> update(
            @PathVariable @Parameter(description = "Identificador único (UUID) do orçamento", example = "6fa85f64-5717-4562-b3fc-2c963f66afa6") UUID id,
            @RequestBody @Valid BudgetUpdateDTO dto,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(budgetService.update(id, user.getId(), dto));
    }

    // ✅ Excluir orçamento (Hard delete definitivo com Zero IDOR)
    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir orçamento", description = "Exclui fisicamente e em definitivo um orçamento pertencente ao usuário autenticado (Hard Delete)")
    public ResponseEntity<Void> delete(
            @PathVariable @Parameter(description = "Identificador único (UUID) do orçamento", example = "6fa85f64-5717-4562-b3fc-2c963f66afa6") UUID id,
            @AuthenticationPrincipal User user
    ) {
        budgetService.delete(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}

