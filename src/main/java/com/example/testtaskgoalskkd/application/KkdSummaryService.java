package com.example.testtaskgoalskkd.application;

/**
 * Сценарии получения метрик качества данных.
 */
public interface KkdSummaryService {

    /**
     * Сводка метрик по подразделению (с учётом вложенных) для прогона
     * в сравнении с предыдущим прогоном.
     *
     * @throws NotFoundException если прогон или подразделение не найдены
     */
    KkdSummary getSummary(String runId, String unitId);
}
