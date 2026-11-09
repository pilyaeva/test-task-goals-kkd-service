package com.example.testtaskgoalskkd.domain.quality;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * KR_WEIGHTS_SUM: сумма весов КР цели не равна 100. Вес null считается нулём.
 * Цель без КР правилом не затрагивается (для неё срабатывает GOAL_NO_KR).
 */
public class KrWeightsSumRule implements QualityRule {

    public static final String CODE = "KR_WEIGHTS_SUM";

    private static final BigDecimal EXPECTED_SUM = BigDecimal.valueOf(100);

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public boolean isViolatedBy(GoalSnapshot goal, LocalDate snapshotDate) {
        if (goal.keyResults().isEmpty()) {
            return false;
        }
        BigDecimal sum = goal.keyResults().stream()
                .map(KeyResultSnapshot::weight)
                .map(weight -> weight == null ? BigDecimal.ZERO : weight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.compareTo(EXPECTED_SUM) != 0;
    }
}
