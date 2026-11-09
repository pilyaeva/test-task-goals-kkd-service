package com.example.testtaskgoalskkd.application;

import java.util.List;

/**
 * Страница проблем прогона.
 */
public record IssuesPage(List<Issue> items, long total, int limit, long offset) {

    /** {@code unit} равен null, если у цели нет исполнителя. */
    public record Issue(String id, String ruleCode, String severity, String goalId, UnitRef unit) {
    }

    public record UnitRef(String id, String name) {
    }
}
