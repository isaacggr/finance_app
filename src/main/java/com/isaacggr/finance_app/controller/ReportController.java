package com.isaacggr.finance_app.controller;

import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.exception.ErrorResponse;
import com.isaacggr.finance_app.service.BillAlertService;
import com.isaacggr.finance_app.service.MonthlyReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/reports")
@Tag(name = "Relatórios", description = "Endpoints para geração e envio de relatórios e extratos financeiros por e-mail")
public class ReportController {

    private final MonthlyReportService monthlyReportService;
    private final BillAlertService billAlertService;

    public ReportController(MonthlyReportService monthlyReportService, BillAlertService billAlertService) {
        this.monthlyReportService = monthlyReportService;
        this.billAlertService = billAlertService;
    }

    @PostMapping("/monthly/send-me")
    @Operation(
            summary = "Enviar relatório mensal por e-mail",
            description = "Consolida receitas, despesas, saldo e maiores gastos do mês selecionado (padrão: mês anterior) e despacha por e-mail para o usuário autenticado"
    )
    @ApiResponse(responseCode = "200", description = "Relatório mensal enviado com sucesso")
    @ApiResponse(responseCode = "409", description = "Conflito ao calcular o período do relatório",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Map<String, String>> sendMonthlyReport(
            @RequestParam(required = false) @Parameter(description = "Mês de referência (1 a 12). Opcional, assume o mês anterior se omitido.", example = "10") Integer month,
            @RequestParam(required = false) @Parameter(description = "Ano de referência (ex: 2026). Opcional, assume o ano atual se omitido.", example = "2026") Integer year,
            @AuthenticationPrincipal User user
    ) {
        monthlyReportService.sendReportForUser(user, month, year);

        String periodMessage = (month != null) ? (month + "/" + (year != null ? year : "atual")) : "do mês anterior";
        return ResponseEntity.ok(Map.of(
                "message", "Relatório financeiro " + periodMessage + " enviado com sucesso para: " + user.getEmail()
        ));
    }

    @PostMapping("/bill-alerts/send-me")
    @Operation(
            summary = "Enviar alerta de contas a vencer por e-mail",
            description = "Verifica despesas agendadas para vencer nos próximos dias (padrão: 3 dias) e envia um alerta preventivo por e-mail para o usuário autenticado"
    )
    public ResponseEntity<Map<String, String>> sendBillAlerts(
            @RequestParam(required = false, defaultValue = "3") @Parameter(description = "Janela de dias à frente para verificação (1 a 30)", example = "3") Integer days,
            @AuthenticationPrincipal User user
    ) {
        int count = billAlertService.sendAlertsForUser(user, days);

        String message = (count > 0)
                ? "Alerta enviado com sucesso com " + count + " conta(s) a vencer nos próximos " + days + " dias."
                : "Você não possui despesas a vencer nos próximos " + days + " dias.";

        return ResponseEntity.ok(Map.of("message", message, "billsFound", String.valueOf(count)));
    }
}

