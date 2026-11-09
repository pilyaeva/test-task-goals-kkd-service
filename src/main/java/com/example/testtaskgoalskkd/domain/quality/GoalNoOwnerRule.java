package com.example.testtaskgoalskkd.domain.quality;

import java.time.LocalDate;

/**
 * GOAL_NO_OWNER: у цели нет ответственного.
 */
public class GoalNoOwnerRule implements QualityRule {

    public static final String CODE = "GOAL_NO_OWNER";

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public boolean isViolatedBy(GoalSnapshot goal, LocalDate snapshotDate) {
        return goal.assigneeEmployeeId() == null || goal.assigneeEmployeeId().isBlank();
    }
}
