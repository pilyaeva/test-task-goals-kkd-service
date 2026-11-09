package com.example.testtask.api.dto;

import com.example.testtask.application.RunListItem;
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
