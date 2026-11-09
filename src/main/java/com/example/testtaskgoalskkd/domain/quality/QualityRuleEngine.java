package com.example.testtaskgoalskkd.domain.quality;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Компонент правил контроля качества данных. Получает цели с ключевыми результатами и дату снимка,
 * возвращает найденные проблемы. Не зависит от API и доступа к данным.
 * Алгоритм — specs/design.md, раздел «Алгоритмы».
 */
public class QualityRuleEngine {

    public static final String ACTIVE_STATUS = "ACTIVE";

    private final List<QualityRule> rules;

    /**
     * Правила в порядке выполнения: он же определяет порядок результата.
     */
    public QualityRuleEngine() {
        this(List.of(
                new GoalNoOwnerRule(),
                new GoalNoKrRule(),
                new KrWeightsSumRule(),
                new KrStaleProgressRule()));
    }

    public QualityRuleEngine(List<QualityRule> rules) {
        this.rules = List.copyOf(rules);
    }

    /**
     * Коды правил, которые выполняет компонент, в порядке выполнения.
     */
    public List<String> ruleCodes() {
        return rules.stream().map(QualityRule::code).toList();
    }

    /**
     * Проверяет активные цели. Результат: проблемы первого правила, затем второго и т. д.,
     * внутри правила — по идентификатору цели. Одно правило — не более одной проблемы на цель.
     */
    public List<DetectedProblem> check(Collection<GoalSnapshot> goals, LocalDate snapshotDate) {
        List<GoalSnapshot> activeGoals = goals.stream()
                .filter(goal -> ACTIVE_STATUS.equals(goal.statusId()))
                .sorted(Comparator.comparing(GoalSnapshot::goalId))
                .toList();

        List<DetectedProblem> problems = new ArrayList<>();
        for (QualityRule rule : rules) {
            for (GoalSnapshot goal : activeGoals) {
                if (rule.isViolatedBy(goal, snapshotDate)) {
                    problems.add(new DetectedProblem(rule.code(), goal.goalId()));
                }
            }
        }
        return problems;
    }
}
