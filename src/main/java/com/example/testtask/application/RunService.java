package com.example.testtask.application;

import java.util.List;

/**
 * Сценарии работы с прогонами.
 */
public interface RunService {

    /**
     * Список прогонов от новых к старым. Каждый прогон сравнивается с ближайшим
     * более ранним; у самого раннего comparedRunId равен null.
     */
    List<RunListItem> getRuns();
}
