package com.example.testtaskgoalskkd.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Цель (таблица goals). Отображены только поля, нужные для контроля качества.
 */
@Entity
@Table(name = "goals")
public class Goal {

    @Id
    private String id;

    @Column(name = "assignee_employee_id")
    private String assigneeEmployeeId;

    @Column(name = "status_id", nullable = false)
    private String statusId;

    protected Goal() {
    }

    public String getStatusId() {
        return statusId;
    }

    public String getId() {
        return id;
    }

    public String getAssigneeEmployeeId() {
        return assigneeEmployeeId;
    }
}
