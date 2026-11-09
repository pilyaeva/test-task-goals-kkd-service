package com.example.testtaskgoalskkd.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.testtaskgoalskkd.domain.Goal;
import com.example.testtaskgoalskkd.domain.Run;
import com.example.testtaskgoalskkd.domain.RunSummary;
import com.example.testtaskgoalskkd.domain.quality.QualityRuleEngine;
import com.example.testtaskgoalskkd.infrastructure.repository.GoalRepository;
import com.example.testtaskgoalskkd.infrastructure.repository.KeyResultRepository;
import com.example.testtaskgoalskkd.infrastructure.repository.RuleRepository;
import com.example.testtaskgoalskkd.infrastructure.repository.RunProblemRepository;
import com.example.testtaskgoalskkd.infrastructure.repository.RunRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RunServiceTest {

    @Mock
    RunRepository runRepository;
    @Mock
    RunProblemRepository runProblemRepository;
    @Mock
    GoalRepository goalRepository;
    @Mock
    KeyResultRepository keyResultRepository;
    @Mock
    RuleRepository ruleRepository;

    static final LocalDate TODAY = LocalDate.parse("2026-11-09");

    RunServiceImpl runService;

    @BeforeEach
    void setUp() {
        runService = new RunServiceImpl(
                runRepository, runProblemRepository, goalRepository, keyResultRepository, ruleRepository,
                new QualityRuleEngine(),
                Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC));
    }

    private static RunSummary run(int id, String date, long problems) {
        return new RunSummary(String.valueOf(id), "COMPLETED", LocalDate.parse(date), problems);
    }

    @Test
    void linksRunToPreviousByDateAndKeepsProblemsCount() {
        when(runRepository.findAllSummaries())
                .thenReturn(List.of(run(2, "2026-11-01", 5), run(1, "2026-10-01", 3)));

        List<RunListItem> result = runService.getRuns();

        assertThat(result).extracting(RunListItem::id).containsExactly("2", "1");
        assertThat(result.get(0).comparedRunId()).isEqualTo("1");
        assertThat(result.get(0).problemsCount()).isEqualTo(5);
        assertThat(result.get(1).comparedRunId()).isNull();
        assertThat(result.get(1).problemsCount()).isEqualTo(3);
    }

    @Test
    void buildsChainForThreeRuns() {
        when(runRepository.findAllSummaries()).thenReturn(
                List.of(run(30, "2026-03-01", 0), run(20, "2026-02-01", 0), run(10, "2026-01-01", 0)));

        List<RunListItem> result = runService.getRuns();

        assertThat(result).extracting(RunListItem::comparedRunId).containsExactly("20", "10", null);
    }

    @Test
    void usesDateNotIdToPickPreviousRun() {
        // репозиторий отдаёт порядок по дате: id=2 новее id=5
        when(runRepository.findAllSummaries())
                .thenReturn(List.of(run(2, "2026-06-01", 1), run(5, "2026-01-01", 1)));

        List<RunListItem> result = runService.getRuns();

        assertThat(result.get(0).comparedRunId()).isEqualTo("5");
        assertThat(result.get(1).comparedRunId()).isNull();
    }

    @Test
    void sameDateRunsFormChainWithoutSelfReference() {
        when(runRepository.findAllSummaries())
                .thenReturn(List.of(run(7, "2026-05-01", 0), run(4, "2026-05-01", 0)));

        List<RunListItem> result = runService.getRuns();

        assertThat(result).allSatisfy(item -> assertThat(item.comparedRunId()).isNotEqualTo(item.id()));
        assertThat(result.get(0).comparedRunId()).isEqualTo("4");
        assertThat(result.get(1).comparedRunId()).isNull();
    }

    @Test
    void returnsZeroProblemsAndNoComparisonForSingleRun() {
        when(runRepository.findAllSummaries()).thenReturn(List.of(run(1, "2026-01-01", 0)));

        List<RunListItem> result = runService.getRuns();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).comparedRunId()).isNull();
        assertThat(result.get(0).problemsCount()).isZero();
    }

    @Test
    void returnsEmptyListWhenNoRuns() {
        when(runRepository.findAllSummaries()).thenReturn(List.of());

        assertThat(runService.getRuns()).isEmpty();
    }

    @Test
    void propagatesRepositoryFailure() {
        when(runRepository.findAllSummaries()).thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> runService.getRuns()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void startRunSavesRunWithTodayAsSnapshotDate() {
        when(goalRepository.findByStatusId("ACTIVE")).thenReturn(List.of());
        when(runRepository.findAllIds()).thenReturn(List.of());
        when(runProblemRepository.findAllIds()).thenReturn(List.of());
        when(runRepository.findAllSummaries())
                .thenReturn(List.of(new RunSummary("RUN-001", "COMPLETED", TODAY, 0)));

        RunListItem result = runService.startRun();

        assertThat(result.id()).isEqualTo("RUN-001");
        assertThat(result.problemsCount()).isZero();
        ArgumentCaptor<Run> saved = ArgumentCaptor.forClass(Run.class);
        verify(runRepository).save(saved.capture());
        assertThat(saved.getValue().getSnapshotDate()).isEqualTo(TODAY);
        assertThat(saved.getValue().getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void startRunFailsWithoutSavingWhenRuleIsMissingInDictionary() {
        Goal goal = mock(Goal.class);
        when(goal.getId()).thenReturn("G-1");
        when(goal.getStatusId()).thenReturn("ACTIVE");
        when(goalRepository.findByStatusId("ACTIVE")).thenReturn(List.of(goal));
        when(keyResultRepository.findByGoalIdIn(List.of("G-1"))).thenReturn(List.of());
        when(ruleRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> runService.startRun()).isInstanceOf(IllegalStateException.class);

        verify(runRepository, never()).save(any());
        verify(runProblemRepository, never()).saveAll(any());
    }
}
