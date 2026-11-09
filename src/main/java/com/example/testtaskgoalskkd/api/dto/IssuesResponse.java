package com.example.testtaskgoalskkd.api.dto;

import com.example.testtaskgoalskkd.application.IssuesPage;
import java.util.List;

/**
 * Ответ GET /api/kkd/issues. Имена полей в JSON — snake_case.
 */
public record IssuesResponse(List<IssueResponse> items, long total, int limit, long offset) {

    public record IssueResponse(String id, String ruleCode, String severity, String goalId, UnitRef unit) {
    }

    public record UnitRef(String id, String name) {
    }

    public static IssuesResponse from(IssuesPage page) {
        return new IssuesResponse(
                page.items().stream()
                        .map(i -> new IssueResponse(
                                i.id(), i.ruleCode(), i.severity(), i.goalId(),
                                i.unit() == null ? null : new UnitRef(i.unit().id(), i.unit().name())))
                        .toList(),
                page.total(), page.limit(), page.offset());
    }
}
