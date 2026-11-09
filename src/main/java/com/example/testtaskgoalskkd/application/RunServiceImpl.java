package com.example.testtaskgoalskkd.application;

import com.example.testtaskgoalskkd.domain.Goal;
import com.example.testtaskgoalskkd.domain.KeyResult;
import com.example.testtaskgoalskkd.domain.Rule;
import com.example.testtaskgoalskkd.domain.Run;
import com.example.testtaskgoalskkd.domain.RunProblem;
import com.example.testtaskgoalskkd.domain.RunSummary;
import com.example.testtaskgoalskkd.domain.quality.DetectedProblem;
import com.example.testtaskgoalskkd.domain.quality.GoalSnapshot;
import com.example.testtaskgoalskkd.domain.quality.KeyResultSnapshot;
import com.example.testtaskgoalskkd.domain.quality.QualityRuleEngine;
import com.example.testtaskgoalskkd.infrastructure.repository.GoalRepository;
import com.example.testtaskgoalskkd.infrastructure.repository.KeyResultRepository;
import com.example.testtaskgoalskkd.infrastructure.repository.RuleRepository;
import com.example.testtaskgoalskkd.infrastructure.repository.RunProblemRepository;
import com.example.testtaskgoalskkd.infrastructure.repository.RunRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RunServiceImpl implements RunService {

    static final String STATUS_COMPLETED = "COMPLETED";

    private final RunRepository runRepository;
    private final RunProblemRepository runProblemRepository;
    private final GoalRepository goalRepository;
    private final KeyResultRepository keyResultRepository;
    private final RuleRepository ruleRepository;
    private final QualityRuleEngine ruleEngine;
    private final Clock clock;

    public RunServiceImpl(
            RunRepository runRepository,
            RunProblemRepository runProblemRepository,
            GoalRepository goalRepository,
            KeyResultRepository keyResultRepository,
            RuleRepository ruleRepository,
            QualityRuleEngine ruleEngine,
            Clock clock) {
        this.runRepository = runRepository;
        this.runProblemRepository = runProblemRepository;
        this.goalRepository = goalRepository;
        this.keyResultRepository = keyResultRepository;
        this.ruleRepository = ruleRepository;
        this.ruleEngine = ruleEngine;
        this.clock = clock;
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
            String comparedRunId = i + 1 < runs.size() ? runs.get(i + 1).id() : null;
            result.add(new RunListItem(
                    run.id(), run.status(), run.snapshotDate(), comparedRunId, run.problemsCount()));
        }
        return result;
    }

    /**
     * Запуск прогона. Алгоритм — specs/design.md, «Запуск прогона»; правила — раздел «Алгоритмы».
     */
    @Override
    @Transactional
    public RunListItem startRun() {
        LocalDate snapshotDate = LocalDate.now(clock);

        List<DetectedProblem> problems = ruleEngine.check(loadActiveGoals(), snapshotDate);
        Map<String, Rule> rulesByCode = ruleRepository.findAll().stream()
                .collect(Collectors.toMap(Rule::getCode, rule -> rule, (first, second) -> first));
        for (DetectedProblem problem : problems) {
            if (!rulesByCode.containsKey(problem.ruleCode())) {
                throw new IllegalStateException("Правило " + problem.ruleCode() + " отсутствует в справочнике rules");
            }
        }

        String runId = new SequentialIds("RUN-", runRepository.findAllIds()).next();
        runRepository.save(new Run(runId, STATUS_COMPLETED, snapshotDate));

        SequentialIds problemIds = new SequentialIds("PRB-", runProblemRepository.findAllIds());
        List<RunProblem> entities = problems.stream()
                .map(problem -> new RunProblem(
                        problemIds.next(), runId, rulesByCode.get(problem.ruleCode()).getId(), problem.goalId()))
                .toList();
        runProblemRepository.saveAll(entities);

        return getRuns().stream()
                .filter(item -> item.id().equals(runId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Созданный прогон " + runId + " не найден"));
    }

    private List<GoalSnapshot> loadActiveGoals() {
        List<Goal> goals = goalRepository.findByStatusId(QualityRuleEngine.ACTIVE_STATUS);
        if (goals.isEmpty()) {
            return List.of();
        }
        Map<String, List<KeyResultSnapshot>> keyResultsByGoal = new HashMap<>();
        for (KeyResult kr : keyResultRepository.findByGoalIdIn(goals.stream().map(Goal::getId).toList())) {
            keyResultsByGoal.computeIfAbsent(kr.getGoalId(), id -> new ArrayList<>())
                    .add(new KeyResultSnapshot(kr.getWeight(), kr.getUpdatedDate()));
        }
        return goals.stream()
                .map(goal -> new GoalSnapshot(
                        goal.getId(),
                        goal.getAssigneeEmployeeId(),
                        goal.getStatusId(),
                        keyResultsByGoal.getOrDefault(goal.getId(), List.of())))
                .toList();
    }
}
