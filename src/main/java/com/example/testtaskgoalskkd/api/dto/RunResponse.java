package com.example.testtaskgoalskkd.api.dto;

import com.example.testtaskgoalskkd.application.RunListItem;
import java.time.LocalDate;

public record RunResponse(
        String id,
        String status,
        LocalDate snapshotDate,
        String comparedRunId,
        int problemsCount) {

    public static RunResponse from(RunListItem item) {
        return new RunResponse(
                item.id(),
                item.status(),
                item.snapshotDate(),
                item.comparedRunId(),
                Math.toIntExact(item.problemsCount()));
    }
}
