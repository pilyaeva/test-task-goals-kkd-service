package com.example.testtaskgoalskkd.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Метрики качества данных по целям подразделения для одного прогона.
 * Значения округлены до 1 знака (HALF_UP).
 */
public record QualityMetrics(
        BigDecimal cleanGoalsShare,
        BigDecimal issuesPerGoal,
        BigDecimal criticalGoalsShare) {

    public static final String CRITICAL = "critical";

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(1);

    public static QualityMetrics calculate(long totalGoals, List<RunProblemView> problems) {
        if (totalGoals == 0) {
            return new QualityMetrics(ZERO, ZERO, ZERO);
        }
        Set<String> goalsWithProblems = new HashSet<>();
        Set<String> goalsWithCritical = new HashSet<>();
        for (RunProblemView problem : problems) {
            goalsWithProblems.add(problem.goalId());
            if (CRITICAL.equals(problem.severity())) {
                goalsWithCritical.add(problem.goalId());
            }
        }
        return new QualityMetrics(
                ratio((totalGoals - goalsWithProblems.size()) * 100, totalGoals),
                ratio(problems.size(), totalGoals),
                ratio(goalsWithCritical.size() * 100L, totalGoals));
    }

    private static BigDecimal ratio(long numerator, long denominator) {
        return BigDecimal.valueOf(numerator).divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP);
    }
}
