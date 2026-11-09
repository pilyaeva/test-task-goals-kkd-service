package com.example.testtaskgoalskkd.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Ключевой результат (таблица key_results). Отображены только поля, нужные для контроля качества.
 */
@Entity
@Table(name = "key_results")
public class KeyResult {

    @Id
    private String id;

    @Column(name = "goal_id", nullable = false)
    private String goalId;

    private BigDecimal weight;

    @Column(name = "updated_date")
    private LocalDate updatedDate;

    protected KeyResult() {
    }

    public String getId() {
        return id;
    }

    public String getGoalId() {
        return goalId;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public LocalDate getUpdatedDate() {
        return updatedDate;
    }
}
