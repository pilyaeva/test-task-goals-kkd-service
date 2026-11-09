package com.example.testtaskgoalskkd.domain.quality;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * KR_STALE_PROGRESS: факт хотя бы одного КР не обновлялся более 45 дней на дату снимка.
 * КР без даты обновления считается устаревшим; дата обновления позже снимка — не устаревший.
 */
public class KrStaleProgressRule implements QualityRule {

    public static final String CODE = "KR_STALE_PROGRESS";

    static final long MAX_DAYS_WITHOUT_UPDATE = 45;

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public boolean isViolatedBy(GoalSnapshot goal, LocalDate snapshotDate) {
        return goal.keyResults().stream().anyMatch(kr -> isStale(kr, snapshotDate));
    }

    private static boolean isStale(KeyResultSnapshot kr, LocalDate snapshotDate) {
        if (kr.updatedDate() == null) {
            return true;
        }
        return ChronoUnit.DAYS.between(kr.updatedDate(), snapshotDate) > MAX_DAYS_WITHOUT_UPDATE;
    }
}
