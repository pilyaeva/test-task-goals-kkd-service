package com.example.testtask.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * Прогон контроля качества данных (таблица runs).
 */
@Entity
@Table(name = "runs")
public class Run {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String status;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    protected Run() {
    }

    public Run(String status, LocalDate snapshotDate) {
        this.status = status;
        this.snapshotDate = snapshotDate;
    }

    public Long getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public LocalDate getSnapshotDate() {
        return snapshotDate;
    }
}
