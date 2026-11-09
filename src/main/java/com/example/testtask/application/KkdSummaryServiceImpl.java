package com.example.testtask.application;

import com.example.testtask.application.KkdSummary.Metric;
import com.example.testtask.application.KkdSummary.ProblemsDelta;
import com.example.testtask.application.KkdSummary.RunRef;
import com.example.testtask.application.KkdSummary.SeverityCount;
import com.example.testtask.application.KkdSummary.Trend;
import com.example.testtask.application.KkdSummary.UnitRef;
import com.example.testtask.domain.Department;
import com.example.testtask.domain.QualityMetrics;
import com.example.testtask.domain.Run;
import com.example.testtask.domain.RunProblemView;
import com.example.testtask.infrastructure.repository.DepartmentRepository;
import com.example.testtask.infrastructure.repository.GoalRepository;
import com.example.testtask.infrastructure.repository.RunProblemRepository;
import com.example.testtask.infrastructure.repository.RunRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KkdSummaryServiceImpl implements KkdSummaryService {

    private static final String CRITICAL = "critical";
    private static final String MAJOR = "major";

    private final RunRepository runRepository;
    private final DepartmentRepository departmentRepository;
    private final GoalRepository goalRepository;
    private final RunProblemRepository runProblemRepository;

    public KkdSummaryServiceImpl(
            RunRepository runRepository,
            DepartmentRepository departmentRepository,
            GoalRepository goalRepository,
            RunProblemRepository runProblemRepository) {
        this.runRepository = runRepository;
        this.departmentRepository = departmentRepository;
        this.goalRepository = goalRepository;
        this.runProblemRepository = runProblemRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public KkdSummary getSummary(String runId, String unitId) {
        Run run = runRepository.findById(runId)
                .orElseThrow(() -> new NotFoundException("Прогон с ID " + runId + " не найден"));
        Optional<Run> previousRun =
                runRepository.findFirstBySnapshotDateLessThanOrderBySnapshotDateDescIdDesc(run.getSnapshotDate());
        Department unit = departmentRepository.findById(unitId)
                .orElseThrow(() -> new NotFoundException("Подразделение с ID " + unitId + " не найдено"));

        List<String> departmentIds = departmentRepository.findSubtreeIds(unitId);
        long totalGoals = goalRepository.countByDepartmentIds(departmentIds);
        List<RunProblemView> current = runProblemRepository.findByRunAndDepartments(run.getId(), departmentIds);
        List<RunProblemView> previous = previousRun
                .map(p -> runProblemRepository.findByRunAndDepartments(p.getId(), departmentIds))
                .orElse(null);

        QualityMetrics currentMetrics = QualityMetrics.calculate(totalGoals, current);
        QualityMetrics previousMetrics = previous == null ? null : QualityMetrics.calculate(totalGoals, previous);

        return new KkdSummary(
                new RunRef(run.getId(), run.getSnapshotDate()),
                previousRun.map(p -> new RunRef(p.getId(), p.getSnapshotDate())).orElse(null),
                new UnitRef(unit.getId(), unit.getName()),
                buildMetrics(currentMetrics, previousMetrics),
                buildSeverities(current, previous),
                buildDelta(current, previous));
    }

    private List<Metric> buildMetrics(QualityMetrics current, QualityMetrics previous) {
        return List.of(
                metric("clean_goals_share", "Доля корректных целей", "%", true,
                        current, previous, QualityMetrics::cleanGoalsShare),
                metric("issues_per_goal", "Проблем на одну цель", "шт.", false,
                        current, previous, QualityMetrics::issuesPerGoal),
                metric("critical_goals_share", "Доля целей с критичными проблемами", "%", false,
                        current, previous, QualityMetrics::criticalGoalsShare));
    }

    private Metric metric(
            String key, String title, String unit, boolean growthIsBetter,
            QualityMetrics current, QualityMetrics previous, Function<QualityMetrics, BigDecimal> getter) {
        BigDecimal value = getter.apply(current);
        if (previous == null) {
            return new Metric(key, title, unit, value, null, null, Trend.SAME);
        }
        BigDecimal previousValue = getter.apply(previous);
        BigDecimal delta = value.subtract(previousValue).setScale(1, RoundingMode.HALF_UP);
        return new Metric(key, title, unit, value, previousValue, delta, trend(delta, growthIsBetter));
    }

    private Trend trend(BigDecimal delta, boolean growthIsBetter) {
        int sign = delta.signum();
        if (sign == 0) {
            return Trend.SAME;
        }
        return (sign > 0) == growthIsBetter ? Trend.BETTER : Trend.WORSE;
    }

    private List<SeverityCount> buildSeverities(List<RunProblemView> current, List<RunProblemView> previous) {
        Map<String, Integer> currentCounts = countBySeverity(current);
        Map<String, Integer> previousCounts = previous == null ? Map.of() : countBySeverity(previous);

        Set<String> codes = new HashSet<>(currentCounts.keySet());
        codes.addAll(previousCounts.keySet());

        List<SeverityCount> result = new ArrayList<>();
        codes.stream().sorted(SEVERITY_ORDER).forEach(code -> {
            int currentCount = currentCounts.getOrDefault(code, 0);
            Integer previousCount = previous == null ? null : previousCounts.getOrDefault(code, 0);
            Integer delta = previousCount == null ? null : currentCount - previousCount;
            result.add(new SeverityCount(code, currentCount, previousCount, delta));
        });
        return result;
    }

    private static final Comparator<String> SEVERITY_ORDER =
            Comparator.comparingInt(KkdSummaryServiceImpl::severityRank).thenComparing(Comparator.naturalOrder());

    private static int severityRank(String severity) {
        if (CRITICAL.equals(severity)) {
            return 0;
        }
        return MAJOR.equals(severity) ? 1 : 2;
    }

    private Map<String, Integer> countBySeverity(List<RunProblemView> problems) {
        Map<String, Integer> counts = new TreeMap<>();
        for (RunProblemView problem : problems) {
            counts.merge(problem.severity(), 1, Integer::sum);
        }
        return counts;
    }

    private ProblemsDelta buildDelta(List<RunProblemView> current, List<RunProblemView> previous) {
        if (previous == null) {
            return new ProblemsDelta(current.size(), 0, 0);
        }
        Set<List<String>> currentKeys = keys(current);
        Set<List<String>> previousKeys = keys(previous);

        int persisting = 0;
        for (List<String> key : currentKeys) {
            if (previousKeys.contains(key)) {
                persisting++;
            }
        }
        return new ProblemsDelta(
                currentKeys.size() - persisting, persisting, previousKeys.size() - persisting);
    }

    /** Проблема идентифицируется парой (rule_id, goal_id). */
    private Set<List<String>> keys(List<RunProblemView> problems) {
        return problems.stream()
                .map(p -> List.of(p.ruleId(), p.goalId()))
                .collect(Collectors.toSet());
    }
}
