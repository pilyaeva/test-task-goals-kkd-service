package com.example.testtask.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import com.example.testtask.domain.Run;
import com.example.testtask.domain.RunProblem;
import com.example.testtask.domain.RunSummary;
import com.example.testtask.infrastructure.repository.RunRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Интеграционный тест репозитория на реальном PostgreSQL 18 (Testcontainers) с миграциями Flyway.
 * HTTP не используется. Требуется запущенный Docker. Каждый тест откатывается.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class RunRepositoryDbTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @Autowired
    RunRepository runRepository;

    @Autowired
    TestEntityManager em;

    @Autowired
    JdbcTemplate jdbc;

    Long ruleId;
    Long goalId;

    @BeforeEach
    void setUp() {
        ruleId = jdbc.queryForObject("INSERT INTO rules (code, severity) VALUES ('GOAL_NO_OWNER', 'critical') RETURNING id", Long.class);
        Long statusId = jdbc.queryForObject("INSERT INTO goal_statuses (name) VALUES ('s') RETURNING id", Long.class);
        goalId = jdbc.queryForObject(
                "INSERT INTO goals (status_id) VALUES (" + statusId + ") RETURNING id", Long.class);
    }

    private Run saveRun(String date) {
        return em.persist(new Run("COMPLETED", LocalDate.parse(date)));
    }

    private void addProblems(Run run, int count) {
        for (int i = 0; i < count; i++) {
            em.persist(new RunProblem(run.getId(), ruleId, goalId));
        }
    }

    @Test
    void countsProblemsPerRunWithoutMixingAndZeroForEmpty() {
        Run a = saveRun("2026-01-01");
        Run b = saveRun("2026-02-01");
        Run empty = saveRun("2026-03-01");
        addProblems(a, 2);
        addProblems(b, 7);
        em.flush();

        List<RunSummary> result = runRepository.findAllSummaries();

        assertThat(result).extracting(RunSummary::id, RunSummary::problemsCount)
                .containsExactly(
                        tuple(empty.getId(), 0L),
                        tuple(b.getId(), 7L),
                        tuple(a.getId(), 2L));
    }

    @Test
    void ordersByDateDescThenIdDesc() {
        Run first = saveRun("2026-01-01");
        Run sameDate1 = saveRun("2026-06-01");
        Run sameDate2 = saveRun("2026-06-01");
        em.flush();

        assertThat(runRepository.findAllSummaries()).extracting(RunSummary::id)
                .containsExactly(sameDate2.getId(), sameDate1.getId(), first.getId());
    }

    @Test
    void returnsEmptyListWhenNoRuns() {
        assertThat(runRepository.findAllSummaries()).isEmpty();
    }
}
