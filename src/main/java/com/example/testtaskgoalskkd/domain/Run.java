package com.example.testtaskgoalskkd.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
    private String id;

    @Column(nullable = false)
    private String status;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    protected Run() {
    }

    public Run(String id, String status, LocalDate snapshotDate) {
        this.id = id;
        this.status = status;
        this.snapshotDate = snapshotDate;
    }

    public String getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public LocalDate getSnapshotDate() {
        return snapshotDate;
    }
}
