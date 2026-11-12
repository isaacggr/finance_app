package com.isaacggr.finance_app.controller;

import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.domain.enums.BillStatus;
import com.isaacggr.finance_app.dto.bill.BillCreateDTO;
import com.isaacggr.finance_app.dto.bill.BillPayRequestDTO;
import com.isaacggr.finance_app.dto.bill.BillPayResponseDTO;
import com.isaacggr.finance_app.dto.bill.BillResponseDTO;
import com.isaacggr.finance_app.dto.bill.BillUpdateDTO;
import com.isaacggr.finance_app.exception.ErrorResponse;
import com.isaacggr.finance_app.service.BillService;
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
@RequestMapping("/bills")
@Tag(name = "Contas a Pagar", description = "Endpoints para gerenciamento de contas a vencer, recorrência mensal/anual e baixa de pagamentos")
public class BillController {

    private final BillService billService;

    public BillController(BillService billService) {
        this.billService = billService;
    }

    @PostMapping
    @Operation(summary = "Cadastrar conta a pagar", description = "Cria uma conta futura ou recorrente associada ao usuário autenticado")
    @ApiResponse(responseCode = "201", description = "Conta a pagar cadastrada com sucesso")
    public ResponseEntity<BillResponseDTO> create(
            @RequestBody @Valid BillCreateDTO dto,
            @AuthenticationPrincipal User user
    ) {
        BillResponseDTO response = billService.create(user, dto);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar contas a pagar", description = "Lista as contas do usuário autenticado com filtro opcional de status (PENDING, OVERDUE ou PAID)")
    public ResponseEntity<List<BillResponseDTO>> list(
            @RequestParam(required = false) @Parameter(description = "Filtro opcional de status (PENDING, OVERDUE ou PAID)", example = "PENDING") BillStatus status,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(billService.listAll(user.getId(), status));
    }

    @GetMapping("/upcoming")
    @Operation(summary = "Listar próximos vencimentos", description = "Retorna contas em aberto (atrasadas e a vencer) até a janela de dias informada no fuso America/Sao_Paulo")
    public ResponseEntity<List<BillResponseDTO>> listUpcoming(
            @RequestParam(required = false, defaultValue = "7") @Parameter(description = "Janela de dias à frente (1 a 90; padrão 7)", example = "7") Integer days,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(billService.listUpcoming(user.getId(), days));
    }

    @GetMapping("/{billId}")
    @Operation(summary = "Buscar conta a pagar por ID", description = "Retorna os detalhes de uma conta pertencente ao usuário autenticado")
    @ApiResponse(responseCode = "200", description = "Conta encontrada")
    @ApiResponse(responseCode = "404", description = "Conta a pagar não encontrada para o usuário autenticado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<BillResponseDTO> getById(
            @PathVariable @Parameter(description = "Identificador único (UUID) da conta", example = "91a85f64-5717-4562-b3fc-2c963f66a111") UUID billId,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(billService.findById(billId, user.getId()));
    }

    @PutMapping("/{billId}")
    @Operation(summary = "Atualizar conta a pagar", description = "Atualiza dados de uma conta ainda não paga (título, descrição, categoria, valor, vencimento e recorrência)")
    @ApiResponse(responseCode = "200", description = "Conta atualizada com sucesso")
    @ApiResponse(responseCode = "404", description = "Conta a pagar não encontrada para o usuário autenticado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Não é possível editar uma conta já marcada como paga",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<BillResponseDTO> update(
            @PathVariable @Parameter(description = "Identificador único (UUID) da conta", example = "91a85f64-5717-4562-b3fc-2c963f66a111") UUID billId,
            @RequestBody @Valid BillUpdateDTO dto,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(billService.update(billId, user.getId(), dto));
    }

    @PatchMapping("/{billId}/pay")
    @Operation(summary = "Pagar conta", description = "Marca a conta como PAID, lança automaticamente a despesa em /transactions (padrão createTransaction=true) e agenda a próxima conta se recorrente")
    @ApiResponse(responseCode = "200", description = "Pagamento registrado com sucesso")
    @ApiResponse(responseCode = "404", description = "Conta a pagar não encontrada para o usuário autenticado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Conta já se encontra marcada como paga",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<BillPayResponseDTO> pay(
            @PathVariable @Parameter(description = "Identificador único (UUID) da conta", example = "91a85f64-5717-4562-b3fc-2c963f66a111") UUID billId,
            @RequestBody(required = false) @Valid BillPayRequestDTO dto,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(billService.pay(billId, user, dto));
    }

    @DeleteMapping("/{billId}")
    @Operation(summary = "Excluir conta a pagar", description = "Exclui fisicamente a conta informada (Hard Delete), preservando eventual transação já gerada")
    @ApiResponse(responseCode = "204", description = "Conta excluída com sucesso")
    @ApiResponse(responseCode = "404", description = "Conta a pagar não encontrada para o usuário autenticado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Void> delete(
            @PathVariable @Parameter(description = "Identificador único (UUID) da conta", example = "91a85f64-5717-4562-b3fc-2c963f66a111") UUID billId,
            @AuthenticationPrincipal User user
    ) {
        billService.delete(billId, user.getId());
        return ResponseEntity.noContent().build();
    }
}
