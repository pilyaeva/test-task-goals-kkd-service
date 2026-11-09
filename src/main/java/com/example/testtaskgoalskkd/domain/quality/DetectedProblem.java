package com.example.testtaskgoalskkd.domain.quality;

/**
 * Проблема, найденная правилом: код правила и цель, на которой оно сработало.
 */
public record DetectedProblem(String ruleCode, String goalId) {
}
