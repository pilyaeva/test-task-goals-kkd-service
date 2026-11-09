package com.example.testtask.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Подразделение (таблица departments).
 */
@Entity
@Table(name = "departments")
public class Department {

    @Id
    private String id;

    @Column(name = "parent_department_id")
    private String parentDepartmentId;

    @Column(nullable = false)
    private String name;

    protected Department() {
    }

    public String getId() {
        return id;
    }

    public String getParentDepartmentId() {
        return parentDepartmentId;
    }

    public String getName() {
        return name;
    }
}
