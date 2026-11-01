package com.isaacggr.finance_app.controller;

import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.dto.DashboardSummaryDTO;
import com.isaacggr.finance_app.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/dashboard")
@Tag(name = "Dashboard", description = "Endpoints para relatórios e inteligência financeira agregada")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Obter resumo financeiro", description = "Retorna métricas consolidadas (total de receitas, despesas, saldo e detalhamento por categoria) para um intervalo de datas ou mês corrente")
    public ResponseEntity<DashboardSummaryDTO> getSummary(
            @RequestParam(required = false) @Parameter(description = "Data inicial do período (AAAA-MM-DD)", example = "2026-11-01") LocalDate startDate,
            @RequestParam(required = false) @Parameter(description = "Data final do período (AAAA-MM-DD)", example = "2026-11-30") LocalDate endDate,
            @AuthenticationPrincipal User user
    ) {
        DashboardSummaryDTO summary = dashboardService.getSummary(user.getId(), startDate, endDate);
        return ResponseEntity.ok(summary);
    }
}
