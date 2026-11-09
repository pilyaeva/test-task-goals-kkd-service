package com.example.testtaskgoalskkd.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import com.example.testtaskgoalskkd.domain.Run;
import com.example.testtaskgoalskkd.domain.RunProblem;
import com.example.testtaskgoalskkd.domain.RunSummary;
import com.example.testtaskgoalskkd.infrastructure.repository.RunRepository;
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
 * Интеграционный тест репозитория на реальном PostgreSQL 18 (Testcontainers) с миграциями Liquibase.
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

    int problemSeq;

    @BeforeEach
    void setUp() {
        jdbc.update("INSERT INTO rules (id, code, severity) VALUES ('RULE-1', 'GOAL_NO_OWNER', 'critical')");
        jdbc.update("INSERT INTO goal_statuses (id, name) VALUES ('ST-1', 's')");
        jdbc.update("INSERT INTO goals (id, status_id) VALUES ('G-1', 'ST-1')");
    }

    private Run saveRun(String id, String date) {
        return em.persist(new Run(id, "COMPLETED", LocalDate.parse(date)));
    }

    private void addProblems(Run run, int count) {
        for (int i = 0; i < count; i++) {
            em.persist(new RunProblem("PRB-" + (++problemSeq), run.getId(), "RULE-1", "G-1"));
        }
    }

    @Test
    void countsProblemsPerRunWithoutMixingAndZeroForEmpty() {
        Run a = saveRun("RUN-A", "2026-01-01");
        Run b = saveRun("RUN-B", "2026-02-01");
        Run empty = saveRun("RUN-C", "2026-03-01");
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
        Run first = saveRun("RUN-001", "2026-01-01");
        Run sameDate1 = saveRun("RUN-002", "2026-06-01");
        Run sameDate2 = saveRun("RUN-003", "2026-06-01");
        em.flush();

        assertThat(runRepository.findAllSummaries()).extracting(RunSummary::id)
                .containsExactly(sameDate2.getId(), sameDate1.getId(), first.getId());
    }

    @Test
    void returnsEmptyListWhenNoRuns() {
        assertThat(runRepository.findAllSummaries()).isEmpty();
    }

    @Test
    void findsClosestEarlierRunByStrictlyLessDate() {
        saveRun("RUN-001", "2026-01-01");
        saveRun("RUN-002", "2026-02-01");
        saveRun("RUN-003", "2026-02-01");
        saveRun("RUN-004", "2026-03-01");
        em.flush();

        // при равных датах строго меньшая дата исключает прогоны с той же датой; берётся больший id
        assertThat(runRepository.findFirstBySnapshotDateLessThanOrderBySnapshotDateDescIdDesc(
                LocalDate.parse("2026-03-01"))).get().extracting(Run::getId).isEqualTo("RUN-003");
        assertThat(runRepository.findFirstBySnapshotDateLessThanOrderBySnapshotDateDescIdDesc(
                LocalDate.parse("2026-02-01"))).get().extracting(Run::getId).isEqualTo("RUN-001");
        assertThat(runRepository.findFirstBySnapshotDateLessThanOrderBySnapshotDateDescIdDesc(
                LocalDate.parse("2026-01-01"))).isEmpty();
    }
}
