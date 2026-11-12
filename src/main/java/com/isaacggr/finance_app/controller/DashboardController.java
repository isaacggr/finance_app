package com.isaacggr.finance_app.controller;

import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.domain.enums.TimeSeriesGranularity;
import com.isaacggr.finance_app.dto.DashboardSummaryDTO;
import com.isaacggr.finance_app.dto.DashboardTimeseriesDTO;
import com.isaacggr.finance_app.exception.ErrorResponse;
import com.isaacggr.finance_app.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
    @Operation(
            summary = "Obter resumo financeiro",
            description = "Retorna métricas consolidadas (receitas, despesas, saldo, detalhamento por categoria) com comparação do período anterior (previous) e variações (variation)"
    )
    @ApiResponse(responseCode = "200", description = "Resumo financeiro retornado com sucesso")
    @ApiResponse(responseCode = "409", description = "Intervalo de datas inválido (startDate posterior a endDate)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<DashboardSummaryDTO> getSummary(
            @RequestParam(required = false) @Parameter(description = "Data inicial do período (AAAA-MM-DD)", example = "2026-11-01") LocalDate startDate,
            @RequestParam(required = false) @Parameter(description = "Data final do período (AAAA-MM-DD)", example = "2026-11-30") LocalDate endDate,
            @AuthenticationPrincipal User user
    ) {
        DashboardSummaryDTO summary = dashboardService.getSummary(user.getId(), startDate, endDate);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/timeseries")
    @Operation(
            summary = "Obter série temporal financeira",
            description = "Retorna pontos contínuos (com zero-filling) de receitas, despesas, saldo do bucket e saldo acumulado para sparklines e gráficos temporais"
    )
    @ApiResponse(responseCode = "200", description = "Série temporal retornada com sucesso")
    @ApiResponse(responseCode = "409", description = "Intervalo de datas inválido (startDate posterior a endDate)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<DashboardTimeseriesDTO> getTimeseries(
            @RequestParam(required = false) @Parameter(description = "Data inicial do período (AAAA-MM-DD)", example = "2026-11-01") LocalDate startDate,
            @RequestParam(required = false) @Parameter(description = "Data final do período (AAAA-MM-DD)", example = "2026-11-30") LocalDate endDate,
            @RequestParam(required = false) @Parameter(description = "Granularidade desejada (DAILY ou MONTHLY). Quando omitida, escolhe automaticamente conforme a largura do intervalo.", example = "DAILY") TimeSeriesGranularity granularity,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(dashboardService.getTimeseries(user.getId(), startDate, endDate, granularity));
    }
}
