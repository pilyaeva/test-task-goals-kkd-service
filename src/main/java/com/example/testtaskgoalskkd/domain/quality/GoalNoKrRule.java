package com.example.testtaskgoalskkd.domain.quality;

import java.time.LocalDate;

/**
 * GOAL_NO_KR: у цели нет ни одного ключевого результата.
 */
public class GoalNoKrRule implements QualityRule {

    public static final String CODE = "GOAL_NO_KR";

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public boolean isViolatedBy(GoalSnapshot goal, LocalDate snapshotDate) {
        return goal.keyResults().isEmpty();
    }
}
