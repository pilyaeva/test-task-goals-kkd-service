package com.example.testtaskgoalskkd.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import com.example.testtaskgoalskkd.application.RunListItem;
import com.example.testtaskgoalskkd.application.RunServiceImpl;
import com.example.testtaskgoalskkd.domain.quality.QualityRuleEngine;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * POST /api/runs на реальном PostgreSQL 18 (сервис + правила + репозитории, без HTTP).
 * Дата «сегодня» зафиксирована: 2026-11-09.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({RunServiceImpl.class, RunStartDbTest.TestBeans.class})
@Testcontainers
class RunStartDbTest {

    static final LocalDate TODAY = LocalDate.parse("2026-11-09");

    @TestConfiguration
    static class TestBeans {
        @Bean
        QualityRuleEngine qualityRuleEngine() {
            return new QualityRuleEngine();
        }

        @Bean
        Clock clock() {
            return Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
        }
    }

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @Autowired
    RunServiceImpl service;
    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc.update("INSERT INTO departments (id, parent_department_id, name) VALUES ('D', NULL, 'D')");
        jdbc.update("INSERT INTO employees (id, department_id, name) VALUES ('E-1', 'D', 'e')");
        jdbc.update("INSERT INTO goal_statuses (id, name) VALUES ('ACTIVE', 'a'), ('DRAFT', 'd')");
        jdbc.update("""
                INSERT INTO rules (id, code, severity) VALUES
                ('RULE-001', 'GOAL_NO_OWNER', 'critical'),
                ('RULE-002', 'GOAL_NO_KR', 'critical'),
                ('RULE-003', 'KR_WEIGHTS_SUM', 'major'),
                ('RULE-004', 'KR_STALE_PROGRESS', 'minor')""");
    }

    private void goal(String id, String owner, String status) {
        jdbc.update("INSERT INTO goals (id, assignee_employee_id, status_id) VALUES (?, ?, ?)", id, owner, status);
    }

    private void kr(String id, String goalId, String weight, String updated) {
        jdbc.update("INSERT INTO key_results (id, goal_id, weight, updated_date) VALUES (?, ?, ?::numeric, ?::date)",
                id, goalId, weight, updated);
    }

    private List<Map<String, Object>> problemsOf(String runId) {
        return jdbc.queryForList(
                "SELECT id, rule_id, goal_id FROM run_problems WHERE run_id = ? ORDER BY id", runId);
    }

    @Test
    void createsRunWithNextNumberAndStoresProblems() {
        goal("G-OLD", "E-1", "DRAFT");
        jdbc.update("INSERT INTO runs (id, status, snapshot_date) VALUES ('RUN-002', 'COMPLETED', '2026-10-26')");
        jdbc.update("INSERT INTO run_problems (id, run_id, rule_id, goal_id) VALUES ('PRB-020', 'RUN-002', 'RULE-001', 'G-OLD')");
        goal("G-1", null, "ACTIVE");                       // GOAL_NO_OWNER + GOAL_NO_KR
        goal("G-2", "E-1", "ACTIVE");
        kr("KR-1", "G-2", "90", "2026-10-30");             // KR_WEIGHTS_SUM
        goal("G-3", "E-1", "ACTIVE");
        kr("KR-2", "G-3", "100", "2026-09-24");            // 2026-11-09 - 46 дней = KR_STALE_PROGRESS

        RunListItem result = service.startRun();

        assertThat(result.id()).isEqualTo("RUN-003");
        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.snapshotDate()).isEqualTo(TODAY);
        assertThat(result.comparedRunId()).isEqualTo("RUN-002");
        assertThat(result.problemsCount()).isEqualTo(4);
        assertThat(problemsOf("RUN-003")).extracting(m -> m.get("id"), m -> m.get("rule_id"), m -> m.get("goal_id"))
                .containsExactly(
                        tuple("PRB-021", "RULE-001", "G-1"),
                        tuple("PRB-022", "RULE-002", "G-1"),
                        tuple("PRB-023", "RULE-003", "G-2"),
                        tuple("PRB-024", "RULE-004", "G-3"));
        assertThat(problemsOf("RUN-002")).hasSize(1);       // старый прогон не затронут
    }

    @Test
    void firstRunGetsNumber001AndNoComparison() {
        goal("G-1", null, "ACTIVE");

        RunListItem result = service.startRun();

        assertThat(result.id()).isEqualTo("RUN-001");
        assertThat(result.snapshotDate()).isEqualTo(TODAY);
        assertThat(result.comparedRunId()).isNull();
        assertThat(problemsOf("RUN-001")).extracting(m -> m.get("id")).containsExactly("PRB-001", "PRB-002");
    }

    @Test
    void savesRunWithoutProblemsWhenNothingIsWrong() {
        goal("G-1", "E-1", "ACTIVE");
        kr("KR-1", "G-1", "50", "2026-11-01");
        kr("KR-2", "G-1", "50", "2026-11-01");
        goal("G-2", null, "DRAFT");                         // неактивная цель не проверяется

        RunListItem result = service.startRun();

        assertThat(result.problemsCount()).isZero();
        assertThat(problemsOf(result.id())).isEmpty();
    }

    @Test
    void savesEmptyRunWhenThereAreNoActiveGoals() {
        RunListItem result = service.startRun();

        assertThat(result.id()).isEqualTo("RUN-001");
        assertThat(result.problemsCount()).isZero();
    }

    @Test
    void repeatedRunsAreSeparateRunsWithSameProblems() {
        goal("G-1", null, "ACTIVE");

        RunListItem first = service.startRun();
        RunListItem second = service.startRun();

        assertThat(first.id()).isEqualTo("RUN-001");
        assertThat(second.id()).isEqualTo("RUN-002");
        assertThat(second.problemsCount()).isEqualTo(first.problemsCount()).isEqualTo(2);
    }

    @Test
    void missingRuleInDictionaryAbortsRunWithoutSavingAnything() {
        jdbc.update("DELETE FROM rules WHERE code = 'GOAL_NO_KR'");
        goal("G-1", "E-1", "ACTIVE");                       // GOAL_NO_KR, правила нет в справочнике

        assertThatThrownBy(() -> service.startRun()).isInstanceOf(IllegalStateException.class);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM runs", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM run_problems", Integer.class)).isZero();
    }
}
