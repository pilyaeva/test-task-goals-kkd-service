package com.example.testtask.api.dto;

import com.example.testtask.application.KkdSummary;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * Ответ GET /api/kkd/summary. Имена полей в JSON — snake_case.
 */
public record KkdSummaryResponse(
        RunRef run,
        RunRef comparedTo,
        UnitRef unit,
        List<MetricResponse> metrics,
        List<SeverityCountResponse> issuesBySeverity,
        DeltaResponse delta) {

    public record RunRef(String id, LocalDate snapshotDate) {
    }

    public record UnitRef(String id, String name) {
    }

    public record MetricResponse(
            String key,
            String title,
            String unit,
            BigDecimal value,
            BigDecimal previous,
            BigDecimal delta,
            String trend) {
    }

    public record SeverityCountResponse(String code, int current, Integer previous, Integer delta) {
    }

    public record DeltaResponse(
            @JsonProperty("new") int created,
            int persisting,
            int resolved) {
    }

    public static KkdSummaryResponse from(KkdSummary summary) {
        return new KkdSummaryResponse(
                runRef(summary.run()),
                runRef(summary.comparedTo()),
                new UnitRef(summary.unit().id(), summary.unit().name()),
                summary.metrics().stream()
                        .map(m -> new MetricResponse(
                                m.key(), m.title(), m.unit(), m.value(), m.previous(), m.delta(),
                                m.trend().name().toLowerCase(Locale.ROOT)))
                        .toList(),
                summary.issuesBySeverity().stream()
                        .map(s -> new SeverityCountResponse(s.code(), s.current(), s.previous(), s.delta()))
                        .toList(),
                new DeltaResponse(
                        summary.delta().created(), summary.delta().persisting(), summary.delta().resolved()));
    }

    private static RunRef runRef(KkdSummary.RunRef run) {
        return run == null ? null : new RunRef(run.id(), run.snapshotDate());
    }
}
