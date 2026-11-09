package com.example.testtask.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class QualityMetricsTest {

    private static RunProblemView problem(String rule, String goal, String severity) {
        return new RunProblemView(rule, goal, severity);
    }

    @Test
    void returnsZerosWhenNoGoals() {
        QualityMetrics metrics = QualityMetrics.calculate(0, List.of());

        assertThat(metrics.cleanGoalsShare()).isEqualByComparingTo("0");
        assertThat(metrics.issuesPerGoal()).isEqualByComparingTo("0");
        assertThat(metrics.criticalGoalsShare()).isEqualByComparingTo("0");
    }

    @Test
    void allGoalsCleanWhenNoProblems() {
        QualityMetrics metrics = QualityMetrics.calculate(4, List.of());

        assertThat(metrics.cleanGoalsShare()).isEqualByComparingTo("100");
        assertThat(metrics.issuesPerGoal()).isEqualByComparingTo("0");
        assertThat(metrics.criticalGoalsShare()).isEqualByComparingTo("0");
    }

    @Test
    void calculatesAllThreeMetrics() {
        // 4 цели; у g1 две проблемы (одна критичная), у g2 одна major
        List<RunProblemView> problems = List.of(
                problem("r1", "g1", "critical"),
                problem("r2", "g1", "major"),
                problem("r2", "g2", "major"));

        QualityMetrics metrics = QualityMetrics.calculate(4, problems);

        assertThat(metrics.cleanGoalsShare()).isEqualByComparingTo("50");
        assertThat(metrics.issuesPerGoal()).isEqualByComparingTo("0.8");
        assertThat(metrics.criticalGoalsShare()).isEqualByComparingTo("25");
    }

    @Test
    void roundsHalfUpToOneDecimal() {
        // 1 проблема на 8 целей = 0.125 -> 0.1; 7/8*100 = 87.5; 1/3 -> 33.3
        assertThat(QualityMetrics.calculate(8, List.of(problem("r", "g", "major"))).issuesPerGoal())
                .isEqualTo(new BigDecimal("0.1"));
        assertThat(QualityMetrics.calculate(3, List.of(problem("r", "g", "critical"))).criticalGoalsShare())
                .isEqualTo(new BigDecimal("33.3"));
        assertThat(QualityMetrics.calculate(16, List.of(problem("r", "g", "major"))).issuesPerGoal())
                .isEqualTo(new BigDecimal("0.1"));
        assertThat(QualityMetrics.calculate(40, List.of(problem("r", "g", "major"))).issuesPerGoal())
                .isEqualTo(new BigDecimal("0.0"));
    }
}
