package com.example.testtask.application;

import java.time.LocalDate;

/**
 * Элемент списка прогонов: прогон, число проблем и прогон, с которым он сравнивается.
 */
public record RunListItem(
        Long id,
        String status,
        LocalDate snapshotDate,
        Long comparedRunId,
        long problemsCount) {
}
