package com.example.testtaskgoalskkd.domain.quality;

import java.time.LocalDate;

/**
 * Правило контроля качества данных. Применяется к одной активной цели.
 */
public interface QualityRule {

    /**
     * Код правила (совпадает с rules.code).
     */
    String code();

    /**
     * Нарушает ли цель правило на дату снимка.
     */
    boolean isViolatedBy(GoalSnapshot goal, LocalDate snapshotDate);
}
