package com.example.testtaskgoalskkd.application;

/**
 * Сценарии получения проблем прогона.
 */
public interface IssuesService {

    /**
     * Проблемы прогона с фильтрами и постраничной выдачей.
     *
     * @param runId    идентификатор прогона (обязателен)
     * @param unitId   подразделение (с вложенными) или null
     * @param ruleCode код правила или null
     * @param severity критичность (critical, major, minor) или null
     * @param limit    размер страницы 1..100, null — 20
     * @param offset   смещение, не меньше 0, null — 0
     * @throws ValidationException если параметры некорректны
     * @throws NotFoundException   если прогон, подразделение или правило не найдены
     */
    IssuesPage getIssues(
            String runId, String unitId, String ruleCode, String severity, Integer limit, Long offset);
}
