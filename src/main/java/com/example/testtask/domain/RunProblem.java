package com.example.testtask.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Проблема, найденная в прогоне (таблица run_problems).
 */
@Entity
@Table(name = "run_problems")
public class RunProblem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", nullable = false)
    private Long runId;

    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    @Column(name = "goal_id", nullable = false)
    private Long goalId;

    protected RunProblem() {
    }

    public RunProblem(Long runId, Long ruleId, Long goalId) {
        this.runId = runId;
        this.ruleId = ruleId;
        this.goalId = goalId;
    }

    public Long getId() {
        return id;
    }

    public Long getRunId() {
        return runId;
    }
}
