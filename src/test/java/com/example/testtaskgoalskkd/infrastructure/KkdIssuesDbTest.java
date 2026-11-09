package com.example.testtaskgoalskkd.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.testtaskgoalskkd.application.IssuesPage;
import com.example.testtaskgoalskkd.application.IssuesPage.Issue;
import com.example.testtaskgoalskkd.application.IssuesServiceImpl;
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
 * GET /api/kkd/issues на реальном PostgreSQL 18 (сервис + репозитории, без HTTP) на данных Ф2
 * из specs/design.md.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(IssuesServiceImpl.class)
@Testcontainers
class KkdIssuesDbTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @Autowired
    IssuesServiceImpl service;
    @Autowired
    JdbcTemplate jdbc;

    /**
     * Ф2: U -> C, отдельное S; G1, G2 — в U, G3 — в C, G4 — в S.
     * RUN-002: P1 (CRIT,G1), P2 (MAJ,G1), P3 (MAJ,G3), P4 (MIN,G2), P5 (CRIT,G4); RUN-001: P6 (CRIT,G1).
     */
    @BeforeEach
    void setUp() {
        jdbc.update("INSERT INTO departments (id, parent_department_id, name) VALUES ('U', NULL, 'Unit U')");
        jdbc.update("INSERT INTO departments (id, parent_department_id, name) VALUES ('C', 'U', 'Unit C')");
        jdbc.update("INSERT INTO departments (id, parent_department_id, name) VALUES ('S', NULL, 'Unit S')");
        jdbc.update("INSERT INTO departments (id, parent_department_id, name) VALUES ('EMPTY', NULL, 'Empty')");
        jdbc.update("INSERT INTO employees (id, department_id, name) VALUES ('E-U', 'U', 'u')");
        jdbc.update("INSERT INTO employees (id, department_id, name) VALUES ('E-C', 'C', 'c')");
        jdbc.update("INSERT INTO employees (id, department_id, name) VALUES ('E-S', 'S', 's')");
        jdbc.update("INSERT INTO goal_statuses (id, name) VALUES ('ST', 'st')");
        goal("G1", "E-U");
        goal("G2", "E-U");
        goal("G3", "E-C");
        goal("G4", "E-S");
        jdbc.update("INSERT INTO rules (id, code, severity) VALUES ('R-CRIT', 'R-CRIT', 'critical')");
        jdbc.update("INSERT INTO rules (id, code, severity) VALUES ('R-MAJ', 'R-MAJ', 'major')");
        jdbc.update("INSERT INTO rules (id, code, severity) VALUES ('R-MAJ2', 'R-MAJ2', 'major')");
        jdbc.update("INSERT INTO rules (id, code, severity) VALUES ('R-MIN', 'R-MIN', 'minor')");
        jdbc.update("INSERT INTO runs (id, status, snapshot_date) VALUES ('RUN-001', 'COMPLETED', '2026-10-19')");
        jdbc.update("INSERT INTO runs (id, status, snapshot_date) VALUES ('RUN-002', 'COMPLETED', '2026-10-26')");
        jdbc.update("INSERT INTO runs (id, status, snapshot_date) VALUES ('RUN-EMPTY', 'COMPLETED', '2026-11-02')");
        problem("P1", "RUN-002", "R-CRIT", "G1");
        problem("P2", "RUN-002", "R-MAJ", "G1");
        problem("P3", "RUN-002", "R-MAJ", "G3");
        problem("P4", "RUN-002", "R-MIN", "G2");
        problem("P5", "RUN-002", "R-CRIT", "G4");
        problem("P6", "RUN-001", "R-CRIT", "G1");
    }

    private void goal(String id, String assignee) {
        jdbc.update("INSERT INTO goals (id, assignee_employee_id, status_id) VALUES (?, ?, 'ST')", id, assignee);
    }

    private void problem(String id, String run, String rule, String goal) {
        jdbc.update("INSERT INTO run_problems (id, run_id, rule_id, goal_id) VALUES (?, ?, ?, ?)",
                id, run, rule, goal);
    }

    private IssuesPage issues(String unit, String rule, String severity, Integer limit, Long offset) {
        return service.getIssues("RUN-002", unit, rule, severity, limit, offset);
    }

    private static java.util.List<String> ids(IssuesPage page) {
        return page.items().stream().map(Issue::id).toList();
    }

    // 1
    @Test
    void withoutFiltersSortsBySeverityThenIdAndIgnoresOtherRuns() {
        IssuesPage page = issues(null, null, null, null, null);

        assertThat(ids(page)).containsExactly("P1", "P5", "P2", "P3", "P4");
        assertThat(page.total()).isEqualTo(5);
        assertThat(page.limit()).isEqualTo(20);
        assertThat(page.offset()).isZero();
    }

    // 2
    @Test
    void itemContainsRuleSeverityGoalAndUnitOfAssignee() {
        Issue p3 = issues(null, null, null, null, null).items().stream()
                .filter(i -> i.id().equals("P3")).findFirst().orElseThrow();

        assertThat(p3.ruleCode()).isEqualTo("R-MAJ");
        assertThat(p3.severity()).isEqualTo("major");
        assertThat(p3.goalId()).isEqualTo("G3");
        assertThat(p3.unit().id()).isEqualTo("C");
        assertThat(p3.unit().name()).isEqualTo("Unit C");
    }

    // 3
    @Test
    void unitFilterIncludesChildUnits() {
        IssuesPage page = issues("U", null, null, null, null);

        assertThat(ids(page)).containsExactly("P1", "P2", "P3", "P4");
        assertThat(page.total()).isEqualTo(4);
    }

    // 4
    @Test
    void childUnitFilterDoesNotCaptureParent() {
        IssuesPage page = issues("C", null, null, null, null);

        assertThat(ids(page)).containsExactly("P3");
        assertThat(page.total()).isEqualTo(1);
    }

    // 5
    @Test
    void ruleCodeFilter() {
        IssuesPage page = issues(null, "R-MAJ", null, null, null);

        assertThat(ids(page)).containsExactly("P2", "P3");
        assertThat(page.total()).isEqualTo(2);
    }

    // 6
    @Test
    void severityFilterCoversAllRulesOfThatSeverity() {
        // в выборке есть проблема только по R-MAJ; R-MAJ2 проверяется через второй прогон ниже
        jdbc.update("INSERT INTO run_problems (id, run_id, rule_id, goal_id) VALUES ('P7', 'RUN-002', 'R-MAJ2', 'G2')");

        IssuesPage page = issues(null, null, "major", null, null);

        assertThat(ids(page)).containsExactly("P2", "P3", "P7");
        assertThat(page.total()).isEqualTo(3);
    }

    // 7
    @Test
    void filtersAreCombinedWithAnd() {
        IssuesPage page = issues("U", null, "critical", null, null);

        assertThat(ids(page)).containsExactly("P1");
        assertThat(page.total()).isEqualTo(1);
    }

    // 8
    @Test
    void firstPage() {
        IssuesPage page = issues(null, null, null, 2, 0L);

        assertThat(ids(page)).containsExactly("P1", "P5");
        assertThat(page.total()).isEqualTo(5);
        assertThat(page.limit()).isEqualTo(2);
        assertThat(page.offset()).isZero();
    }

    // 9
    @Test
    void nextPagesDoNotOverlap() {
        IssuesPage second = issues(null, null, null, 2, 2L);
        IssuesPage third = issues(null, null, null, 2, 4L);

        assertThat(ids(second)).containsExactly("P2", "P3");
        assertThat(ids(third)).containsExactly("P4");
        assertThat(second.total()).isEqualTo(5);
        assertThat(third.total()).isEqualTo(5);
    }

    @Test
    void offsetNotMultipleOfLimitSkipsExactlyOffsetRecords() {
        assertThat(ids(issues(null, null, null, 2, 1L))).containsExactly("P5", "P2");
    }

    // 11
    @Test
    void boundaryLimits() {
        assertThat(issues(null, null, null, 1, null).items()).hasSize(1);
        assertThat(issues(null, null, null, 100, null).items()).hasSize(5);
    }

    // 12
    @Test
    void offsetBeyondResultReturnsEmptyItemsAndKeepsTotal() {
        for (long offset : new long[] {5, 100}) {
            IssuesPage page = issues(null, null, null, null, offset);

            assertThat(page.items()).isEmpty();
            assertThat(page.total()).isEqualTo(5);
        }
    }

    // 13
    @Test
    void runWithoutProblemsReturnsEmptyPage() {
        IssuesPage page = service.getIssues("RUN-EMPTY", null, null, null, null, null);

        assertThat(page.items()).isEmpty();
        assertThat(page.total()).isZero();
    }

    // 14
    @Test
    void filtersWithoutMatchReturnEmptyPage() {
        IssuesPage page = issues("C", null, "critical", null, null);

        assertThat(page.items()).isEmpty();
        assertThat(page.total()).isZero();
    }

    // 15
    @Test
    void goalWithoutAssigneeHasNullUnitAndIsExcludedByUnitFilter() {
        jdbc.update("INSERT INTO goals (id, assignee_employee_id, status_id) VALUES ('G5', NULL, 'ST')");
        problem("P8", "RUN-002", "R-CRIT", "G5");

        IssuesPage all = issues(null, null, null, null, null);
        IssuesPage byUnit = issues("U", null, null, null, null);

        assertThat(all.items()).filteredOn(i -> i.id().equals("P8")).singleElement()
                .satisfies(i -> assertThat(i.unit()).isNull());
        assertThat(all.total()).isEqualTo(6);
        assertThat(ids(byUnit)).doesNotContain("P8");
    }

    // 16
    @Test
    void unitWithoutGoalsReturnsEmptyPage() {
        IssuesPage page = issues("EMPTY", null, null, null, null);

        assertThat(page.items()).isEmpty();
        assertThat(page.total()).isZero();
    }

    // 17
    @Test
    void severalProblemsOnOneGoalAreSeparateItems() {
        assertThat(issues(null, null, null, null, null).items())
                .filteredOn(i -> i.goalId().equals("G1"))
                .extracting(Issue::id).containsExactly("P1", "P2");
    }
}
