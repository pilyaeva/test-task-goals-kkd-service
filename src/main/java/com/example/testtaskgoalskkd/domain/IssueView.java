package com.example.testtaskgoalskkd.domain;

/**
 * Проблема прогона с кодом и критичностью правила и подразделением исполнителя цели.
 * {@code unitId} и {@code unitName} равны null, если у цели нет исполнителя.
 */
public record IssueView(
        String id, String ruleCode, String severity, String goalId, String unitId, String unitName) {
}
