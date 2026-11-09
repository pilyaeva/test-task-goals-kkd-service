package com.example.testtaskgoalskkd.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.testtaskgoalskkd.application.KkdSummary;
import com.example.testtaskgoalskkd.application.KkdSummary.Metric;
import com.example.testtaskgoalskkd.application.KkdSummary.SeverityCount;
import com.example.testtaskgoalskkd.application.KkdSummary.Trend;
import com.example.testtaskgoalskkd.application.KkdSummaryServiceImpl;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Сценарии GET /api/kkd/summary из specs/design.md на реальном PostgreSQL 18
 * (сервис + репозитории, без HTTP). Базовые данные Ф1: подразделение {@code U} без дочерних
 * с целями G1–G4, правила R-CRIT / R-MAJ / R-MAJ2, прогоны RUN-001 (2026-10-19) и RUN-002 (2026-10-26).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(KkdSummaryServiceImpl.class)
@Testcontainers
class KkdSummaryDbTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @Autowired
    KkdSummaryServiceImpl service;
    @Autowired
    JdbcTemplate jdbc;

    int problemSeq;

    @BeforeEach
    void setUp() {
        jdbc.update("INSERT INTO goal_statuses (id, name) VALUES ('ST', 'st')");
        department("U", null);
        employee("E-U", "U");
        for (String goal : List.of("G1", "G2", "G3", "G4")) {
            goal(goal, "E-U");
        }
        jdbc.update("INSERT INTO rules (id, code, severity) VALUES ('R-CRIT', 'R-CRIT', 'critical')");
        jdbc.update("INSERT INTO rules (id, code, severity) VALUES ('R-MAJ', 'R-MAJ', 'major')");
        jdbc.update("INSERT INTO rules (id, code, severity) VALUES ('R-MAJ2', 'R-MAJ2', 'major')");
        run("RUN-001", "2026-10-19");
        run("RUN-002", "2026-10-26");
    }

    private void department(String id, String parent) {
        jdbc.update("INSERT INTO departments (id, parent_department_id, name) VALUES (?, ?, ?)",
                id, parent, "Unit " + id);
    }

    private void employee(String id, String department) {
        jdbc.update("INSERT INTO employees (id, department_id, name) VALUES (?, ?, 'n')", id, department);
    }

    private void goal(String id, String assignee) {
        jdbc.update("INSERT INTO goals (id, assignee_employee_id, status_id) VALUES (?, ?, 'ST')", id, assignee);
    }

    private void run(String id, String date) {
        jdbc.update("INSERT INTO runs (id, status, snapshot_date) VALUES (?, 'COMPLETED', ?::date)", id, date);
    }

    /** Проблемы в формате «правило:цель». */
    private void problems(String run, String... ruleAndGoal) {
        for (String pair : ruleAndGoal) {
            String[] parts = pair.split(":");
            jdbc.update("INSERT INTO run_problems (id, run_id, rule_id, goal_id) VALUES (?, ?, ?, ?)",
                    "PRB-" + (++problemSeq), run, parts[0], parts[1]);
        }
    }

    private KkdSummary summary() {
        return service.getSummary("RUN-002", "U");
    }

    private static Metric metric(KkdSummary summary, String key) {
        return summary.metrics().stream().filter(m -> m.key().equals(key)).findFirst().orElseThrow();
    }

    private static void assertMetric(
            Metric metric, String value, String previous, String delta, Trend trend) {
        assertThat(metric.value()).isEqualByComparingTo(value);
        assertThat(metric.previous()).isEqualByComparingTo(previous);
        assertThat(metric.delta()).isEqualByComparingTo(delta);
        assertThat(metric.trend()).isEqualTo(trend);
    }

    private static void assertSeverity(
            SeverityCount severity, String code, int current, int previous, int delta) {
        assertThat(severity.code()).isEqualTo(code);
        assertThat(severity.current()).isEqualTo(current);
        assertThat(severity.previous()).isEqualTo(previous);
        assertThat(severity.delta()).isEqualTo(delta);
    }

    // 1
    @Test
    void improvementWithDepartmentHierarchyPreviousRunByDateAndFiltering() {
        // U -> C; соседнее S; цели G1, G2 — в U, G3, G4 — в C (переопределяем Ф1), G5 — в S
        jdbc.update("DELETE FROM goals");
        department("C", "U");
        department("S", null);
        employee("E-C", "C");
        employee("E-S", "S");
        goal("G1", "E-U");
        goal("G2", "E-U");
        goal("G3", "E-C");
        goal("G4", "E-C");
        goal("G5", "E-S");
        run("RUN-000", "2026-10-12");
        run("RUN-005", "2026-10-19");
        run("RUN-003", "2026-11-02");
        problems("RUN-000", "R-MAJ:G1", "R-MAJ:G2", "R-MAJ:G3", "R-MAJ:G4");
        problems("RUN-003", "R-MAJ:G1", "R-MAJ:G2", "R-MAJ:G3", "R-MAJ:G4");
        problems("RUN-002", "R-CRIT:G1", "R-MAJ:G3", "R-CRIT:G5");
        problems("RUN-005", "R-CRIT:G1", "R-CRIT:G2", "R-MAJ:G3", "R-CRIT:G5");

        KkdSummary result = service.getSummary("RUN-002", "U");

        assertThat(result.comparedTo().id()).isEqualTo("RUN-005");
        assertThat(result.metrics()).extracting(Metric::key)
                .containsExactly("clean_goals_share", "issues_per_goal", "critical_goals_share");
        assertMetric(metric(result, "clean_goals_share"), "50.0", "25.0", "25.0", Trend.BETTER);
        assertMetric(metric(result, "issues_per_goal"), "0.5", "0.8", "-0.3", Trend.BETTER);
        assertMetric(metric(result, "critical_goals_share"), "25.0", "50.0", "-25.0", Trend.BETTER);
        assertSeverity(result.issuesBySeverity().get(0), "critical", 1, 2, -1);
        assertSeverity(result.issuesBySeverity().get(1), "major", 1, 1, 0);
        assertThat(result.delta().created()).isZero();
        assertThat(result.delta().persisting()).isEqualTo(2);
        assertThat(result.delta().resolved()).isEqualTo(1);

        KkdSummary child = service.getSummary("RUN-002", "C");

        assertThat(child.metrics().get(0).value()).isEqualByComparingTo("50.0");
        assertThat(child.metrics().get(1).value()).isEqualByComparingTo("0.5");
        assertThat(child.metrics().get(2).value()).isEqualByComparingTo("0.0");
        assertThat(child.metrics()).allSatisfy(m -> assertThat(m.trend()).isEqualTo(Trend.SAME));
        assertThat(child.delta().created()).isZero();
        assertThat(child.delta().persisting()).isEqualTo(1);
        assertThat(child.delta().resolved()).isZero();
    }

    // 2
    @Test
    void worseningAndMixedTrends() {
        problems("RUN-002", "R-CRIT:G1", "R-MAJ:G1", "R-MAJ2:G1");
        problems("RUN-001", "R-MAJ:G1", "R-MAJ:G2");

        KkdSummary result = summary();

        assertMetric(metric(result, "clean_goals_share"), "75.0", "50.0", "25.0", Trend.BETTER);
        assertMetric(metric(result, "issues_per_goal"), "0.8", "0.5", "0.3", Trend.WORSE);
        assertMetric(metric(result, "critical_goals_share"), "25.0", "0.0", "25.0", Trend.WORSE);
        assertSeverity(result.issuesBySeverity().get(0), "critical", 1, 0, 1);
        assertSeverity(result.issuesBySeverity().get(1), "major", 2, 2, 0);
        assertThat(result.delta().created()).isEqualTo(2);
        assertThat(result.delta().persisting()).isEqualTo(1);
        assertThat(result.delta().resolved()).isEqualTo(1);
    }

    // 3
    @Test
    void noPreviousRunAndHalfUpRounding() {
        jdbc.update("DELETE FROM runs WHERE id = 'RUN-001'");
        problems("RUN-002", "R-CRIT:G1");

        KkdSummary result = summary();

        assertThat(result.comparedTo()).isNull();
        assertThat(metric(result, "clean_goals_share").value()).isEqualByComparingTo("75.0");
        assertThat(metric(result, "issues_per_goal").value()).isEqualTo(new BigDecimal("0.3"));
        assertThat(metric(result, "critical_goals_share").value()).isEqualByComparingTo("25.0");
        assertThat(result.metrics()).allSatisfy(m -> {
            assertThat(m.previous()).isNull();
            assertThat(m.delta()).isNull();
            assertThat(m.trend()).isEqualTo(Trend.SAME);
        });
        SeverityCount critical = result.issuesBySeverity().get(0);
        assertThat(critical.code()).isEqualTo("critical");
        assertThat(critical.current()).isEqualTo(1);
        assertThat(critical.previous()).isNull();
        assertThat(critical.delta()).isNull();
        assertThat(result.delta().created()).isEqualTo(1);
        assertThat(result.delta().persisting()).isZero();
        assertThat(result.delta().resolved()).isZero();
    }

    // 4
    @Test
    void unitWithoutGoalsGivesZeroes() {
        department("EMPTY", null);

        KkdSummary result = service.getSummary("RUN-002", "EMPTY");

        assertThat(result.metrics()).allSatisfy(m -> {
            assertThat(m.value()).isEqualByComparingTo("0.0");
            assertThat(m.previous()).isEqualByComparingTo("0.0");
            assertThat(m.delta()).isEqualByComparingTo("0.0");
            assertThat(m.trend()).isEqualTo(Trend.SAME);
        });
        assertThat(result.issuesBySeverity()).isEmpty();
        assertThat(result.delta().created()).isZero();
        assertThat(result.delta().persisting()).isZero();
        assertThat(result.delta().resolved()).isZero();
    }

    // 5
    @Test
    void noProblemsInBothRuns() {
        KkdSummary result = summary();

        assertThat(metric(result, "clean_goals_share").value()).isEqualByComparingTo("100.0");
        assertThat(metric(result, "issues_per_goal").value()).isEqualByComparingTo("0.0");
        assertThat(metric(result, "critical_goals_share").value()).isEqualByComparingTo("0.0");
        assertThat(result.issuesBySeverity()).isEmpty();
        assertThat(result.delta().created()).isZero();
        assertThat(result.delta().persisting()).isZero();
        assertThat(result.delta().resolved()).isZero();
    }

    // 6
    @Test
    void allGoalsWithProblems() {
        problems("RUN-002", "R-MAJ:G1", "R-MAJ:G2", "R-MAJ:G3", "R-MAJ:G4");

        assertThat(metric(summary(), "clean_goals_share").value()).isEqualByComparingTo("0.0");
    }

    // 7
    @Test
    void severalProblemsOnOneGoalCountGoalsAndProblemsSeparately() {
        problems("RUN-002", "R-CRIT:G1", "R-MAJ:G1", "R-MAJ2:G1");

        KkdSummary result = summary();

        assertThat(metric(result, "clean_goals_share").value()).isEqualByComparingTo("75.0");
        assertThat(metric(result, "issues_per_goal").value()).isEqualByComparingTo("0.8");
        assertThat(metric(result, "critical_goals_share").value()).isEqualByComparingTo("25.0");
    }

    // 8
    @Test
    void severalCriticalProblemsOnOneGoalCountGoalOnce() {
        jdbc.update("INSERT INTO rules (id, code, severity) VALUES ('R-CRIT2', 'R-CRIT2', 'critical')");
        problems("RUN-002", "R-CRIT:G1", "R-CRIT2:G1");

        KkdSummary result = summary();

        assertThat(metric(result, "critical_goals_share").value()).isEqualByComparingTo("25.0");
        assertThat(result.issuesBySeverity().get(0).code()).isEqualTo("critical");
        assertThat(result.issuesBySeverity().get(0).current()).isEqualTo(2);
    }

    // 9
    @Test
    void sameRuleOnAnotherGoalIsAnotherProblem() {
        problems("RUN-001", "R-MAJ:G1");
        problems("RUN-002", "R-MAJ:G2");

        KkdSummary result = summary();

        assertThat(result.delta().created()).isEqualTo(1);
        assertThat(result.delta().persisting()).isZero();
        assertThat(result.delta().resolved()).isEqualTo(1);
    }

    // 10
    @Test
    void sameRuleOnSameGoalPersists() {
        problems("RUN-001", "R-MAJ:G1");
        problems("RUN-002", "R-MAJ:G1");

        KkdSummary result = summary();

        assertThat(result.delta().created()).isZero();
        assertThat(result.delta().persisting()).isEqualTo(1);
        assertThat(result.delta().resolved()).isZero();
    }

    // 11
    @Test
    void currentRunWithoutProblemsPreviousWithProblems() {
        problems("RUN-001", "R-MAJ:G1", "R-MAJ:G2");

        KkdSummary result = summary();

        assertMetric(metric(result, "clean_goals_share"), "100.0", "50.0", "50.0", Trend.BETTER);
        assertThat(result.delta().created()).isZero();
        assertThat(result.delta().persisting()).isZero();
        assertThat(result.delta().resolved()).isEqualTo(2);
    }

    // 12
    @Test
    void previousRunWithoutProblemsCurrentWithProblems() {
        problems("RUN-002", "R-MAJ:G1", "R-MAJ:G2");

        KkdSummary result = summary();

        assertThat(result.delta().created()).isEqualTo(2);
        assertThat(result.delta().persisting()).isZero();
        assertThat(result.delta().resolved()).isZero();
        assertThat(result.issuesBySeverity()).singleElement()
                .satisfies(s -> assertThat(s.previous()).isZero());
    }

    // 13
    @Test
    void runWithSameSnapshotDateIsNotPrevious() {
        jdbc.update("UPDATE runs SET snapshot_date = '2026-10-26' WHERE id = 'RUN-001'");

        assertThat(summary().comparedTo()).isNull();
    }

    // 14
    @Test
    void earliestRunHasNoComparison() {
        KkdSummary result = service.getSummary("RUN-001", "U");

        assertThat(result.comparedTo()).isNull();
        assertThat(result.metrics()).allSatisfy(m -> assertThat(m.previous()).isNull());
    }

    // 15
    @Test
    void leafUnitUsesOnlyOwnGoals() {
        department("OTHER", null);
        employee("E-OTHER", "OTHER");
        goal("G9", "E-OTHER");
        problems("RUN-002", "R-MAJ:G9");

        KkdSummary result = summary();

        assertThat(metric(result, "clean_goals_share").value()).isEqualByComparingTo("100.0");
        assertThat(result.issuesBySeverity()).isEmpty();
    }

    // 16
    @Test
    void nestedGoalsAreCountedSeparately() {
        jdbc.update("UPDATE goals SET parent_goal_id = 'G1' WHERE id = 'G2'");
        problems("RUN-002", "R-MAJ:G1");

        KkdSummary result = summary();

        assertThat(metric(result, "clean_goals_share").value()).isEqualByComparingTo("75.0");
        assertThat(metric(result, "issues_per_goal").value()).isEqualByComparingTo("0.3");
    }
}
