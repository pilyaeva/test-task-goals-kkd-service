package com.example.testtask.application;

import com.example.testtask.domain.RunSummary;
import com.example.testtask.infrastructure.repository.RunRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RunServiceImpl implements RunService {

    private final RunRepository runRepository;

    public RunServiceImpl(RunRepository runRepository) {
        this.runRepository = runRepository;
    }

    /**
     * Список прогонов от новых к старым. Каждый прогон сравнивается с ближайшим
     * более ранним (следующим в отсортированном списке); у самого раннего — null.
     */
    @Override
    @Transactional(readOnly = true)
    public List<RunListItem> getRuns() {
        List<RunSummary> runs = runRepository.findAllSummaries();
        List<RunListItem> result = new ArrayList<>(runs.size());
        for (int i = 0; i < runs.size(); i++) {
            RunSummary run = runs.get(i);
            Long comparedRunId = i + 1 < runs.size() ? runs.get(i + 1).id() : null;
            result.add(new RunListItem(
                    run.id(), run.status(), run.snapshotDate(), comparedRunId, run.problemsCount()));
        }
        return result;
    }
}
