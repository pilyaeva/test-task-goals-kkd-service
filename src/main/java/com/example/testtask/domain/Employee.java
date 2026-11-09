package com.example.testtask.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Сотрудник (таблица employees).
 */
@Entity
@Table(name = "employees")
public class Employee {

    @Id
    private String id;

    @Column(name = "department_id", nullable = false)
    private String departmentId;

    @Column(nullable = false)
    private String name;

    protected Employee() {
    }

    public String getId() {
        return id;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public String getName() {
        return name;
    }
}
