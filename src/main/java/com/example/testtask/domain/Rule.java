package com.example.testtask.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Правило проверки (таблица rules).
 */
@Entity
@Table(name = "rules")
public class Rule {

    @Id
    private String id;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String severity;

    protected Rule() {
    }

    public String getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getSeverity() {
        return severity;
    }
}
