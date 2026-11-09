package com.example.testtask.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.testtask.domain.RunSummary;
import com.example.testtask.infrastructure.repository.RunRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RunServiceTest {

    @Mock
    RunRepository runRepository;

    @InjectMocks
    RunServiceImpl runService;

    private static RunSummary run(long id, String date, long problems) {
        return new RunSummary(id, "COMPLETED", LocalDate.parse(date), problems);
    }

    @Test
    void linksRunToPreviousByDateAndKeepsProblemsCount() {
        when(runRepository.findAllSummaries())
                .thenReturn(List.of(run(2, "2026-11-01", 5), run(1, "2026-10-01", 3)));

        List<RunListItem> result = runService.getRuns();

        assertThat(result).extracting(RunListItem::id).containsExactly(2L, 1L);
        assertThat(result.get(0).comparedRunId()).isEqualTo(1L);
        assertThat(result.get(0).problemsCount()).isEqualTo(5);
        assertThat(result.get(1).comparedRunId()).isNull();
        assertThat(result.get(1).problemsCount()).isEqualTo(3);
    }

    @Test
    void buildsChainForThreeRuns() {
        when(runRepository.findAllSummaries()).thenReturn(
                List.of(run(30, "2026-03-01", 0), run(20, "2026-02-01", 0), run(10, "2026-01-01", 0)));

        List<RunListItem> result = runService.getRuns();

        assertThat(result).extracting(RunListItem::comparedRunId).containsExactly(20L, 10L, null);
    }

    @Test
    void usesDateNotIdToPickPreviousRun() {
        // репозиторий отдаёт порядок по дате: id=2 новее id=5
        when(runRepository.findAllSummaries())
                .thenReturn(List.of(run(2, "2026-06-01", 1), run(5, "2026-01-01", 1)));

        List<RunListItem> result = runService.getRuns();

        assertThat(result.get(0).comparedRunId()).isEqualTo(5L);
        assertThat(result.get(1).comparedRunId()).isNull();
    }

    @Test
    void sameDateRunsFormChainWithoutSelfReference() {
        when(runRepository.findAllSummaries())
                .thenReturn(List.of(run(7, "2026-05-01", 0), run(4, "2026-05-01", 0)));

        List<RunListItem> result = runService.getRuns();

        assertThat(result).allSatisfy(item -> assertThat(item.comparedRunId()).isNotEqualTo(item.id()));
        assertThat(result.get(0).comparedRunId()).isEqualTo(4L);
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
}
