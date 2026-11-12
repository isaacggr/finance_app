package com.isaacggr.finance_app.dto;

import com.isaacggr.finance_app.domain.enums.TimeSeriesGranularity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

public record DashboardTimeseriesDTO(
        @Schema(description = "Data inicial da série temporal", example = "2026-11-01")
        LocalDate startDate,

        @Schema(description = "Data final da série temporal", example = "2026-11-30")
        LocalDate endDate,

        @Schema(description = "Granularidade utilizada na agregação dos pontos", example = "DAILY")
        TimeSeriesGranularity granularity,

        @Schema(description = "Pontos contínuos do período com zeros preenchidos para datas sem movimentação")
        List<DashboardTimeseriesPointDTO> points
) {
}
