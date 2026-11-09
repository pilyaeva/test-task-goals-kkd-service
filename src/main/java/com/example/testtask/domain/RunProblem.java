package com.example.testtask.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Проблема, найденная в прогоне (таблица run_problems).
 */
@Entity
@Table(name = "run_problems")
public class RunProblem {

    @Id
    private String id;

    @Column(name = "run_id", nullable = false)
    private String runId;

    @Column(name = "rule_id", nullable = false)
    private String ruleId;

    @Column(name = "goal_id", nullable = false)
    private String goalId;

    protected RunProblem() {
    }

    public RunProblem(String id, String runId, String ruleId, String goalId) {
        this.id = id;
        this.runId = runId;
        this.ruleId = ruleId;
        this.goalId = goalId;
    }

    public String getId() {
        return id;
    }

    public String getRunId() {
        return runId;
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getGoalId() {
        return goalId;
    }
}
