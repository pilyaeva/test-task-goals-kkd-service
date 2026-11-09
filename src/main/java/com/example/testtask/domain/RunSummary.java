package com.example.testtask.domain;

import java.time.LocalDate;

/**
 * Прогон с числом найденных проблем.
 */
public record RunSummary(Long id, String status, LocalDate snapshotDate, long problemsCount) {
}
