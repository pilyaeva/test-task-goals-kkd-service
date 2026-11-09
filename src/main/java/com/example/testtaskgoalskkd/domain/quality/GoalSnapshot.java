package com.example.testtaskgoalskkd.domain.quality;

import java.util.List;

/**
 * Цель с ключевыми результатами — вход компонента правил.
 *
 * @param goalId             идентификатор цели
 * @param assigneeEmployeeId ответственный; может быть null
 * @param statusId           статус цели
 * @param keyResults         ключевые результаты цели (пустой список, если их нет)
 */
public record GoalSnapshot(
        String goalId,
        String assigneeEmployeeId,
        String statusId,
        List<KeyResultSnapshot> keyResults) {

    public GoalSnapshot {
        keyResults = keyResults == null ? List.of() : List.copyOf(keyResults);
    }
}
