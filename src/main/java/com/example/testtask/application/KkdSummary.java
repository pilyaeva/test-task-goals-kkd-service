package com.example.testtask.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Сводка метрик качества данных по целям и ключевым результатам подразделения.
 */
public record KkdSummary(
        RunRef run,
        RunRef comparedTo,
        UnitRef unit,
        List<Metric> metrics,
        List<SeverityCount> issuesBySeverity,
        ProblemsDelta delta) {

    public record RunRef(String id, LocalDate snapshotDate) {
    }

    public record UnitRef(String id, String name) {
    }

    public enum Trend { BETTER, WORSE, SAME }

    public record Metric(
            String key,
            String title,
            String unit,
            BigDecimal value,
            BigDecimal previous,
            BigDecimal delta,
            Trend trend) {
    }

    public record SeverityCount(String code, int current, Integer previous, Integer delta) {
    }

    public record ProblemsDelta(int created, int persisting, int resolved) {
    }
}
