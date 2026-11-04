package com.isaacggr.finance_app.controller;

import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.service.MonthlyReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

    public ReportController(MonthlyReportService monthlyReportService) {
        this.monthlyReportService = monthlyReportService;
    }

    @PostMapping("/monthly/send-me")
    @Operation(
            summary = "Enviar relatório mensal por e-mail",
            description = "Consolida receitas, despesas, saldo e maiores gastos do mês selecionado (padrão: mês anterior) e despacha por e-mail para o usuário autenticado"
    )
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
}

