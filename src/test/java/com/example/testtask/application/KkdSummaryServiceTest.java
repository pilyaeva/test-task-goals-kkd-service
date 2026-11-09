package com.example.testtask.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.testtask.application.KkdSummary.Metric;
import com.example.testtask.application.KkdSummary.Trend;
import com.example.testtask.domain.Department;
import com.example.testtask.domain.Run;
import com.example.testtask.domain.RunProblemView;
import com.example.testtask.infrastructure.repository.DepartmentRepository;
import com.example.testtask.infrastructure.repository.GoalRepository;
import com.example.testtask.infrastructure.repository.RunProblemRepository;
import com.example.testtask.infrastructure.repository.RunRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KkdSummaryServiceTest {

    private static final LocalDate CURRENT_DATE = LocalDate.parse("2026-10-26");
    private static final LocalDate PREVIOUS_DATE = LocalDate.parse("2026-10-19");
    private static final List<String> DEPARTMENTS = List.of("HQ-CIB", "HQ-CIB-1");

    @Mock
    RunRepository runRepository;
    @Mock
    DepartmentRepository departmentRepository;
    @Mock
    GoalRepository goalRepository;
    @Mock
    RunProblemRepository runProblemRepository;

    @InjectMocks
    KkdSummaryServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        when(runRepository.findById("RUN-008"))
                .thenReturn(Optional.of(new Run("RUN-008", "COMPLETED", CURRENT_DATE)));
        when(runRepository.findFirstBySnapshotDateLessThanOrderBySnapshotDateDescIdDesc(CURRENT_DATE))
                .thenReturn(Optional.of(new Run("RUN-007", "COMPLETED", PREVIOUS_DATE)));
        when(departmentRepository.findById("HQ-CIB")).thenReturn(Optional.of(department("HQ-CIB", "Департамент")));
        when(departmentRepository.findSubtreeIds("HQ-CIB")).thenReturn(DEPARTMENTS);
        when(goalRepository.countByDepartmentIds(DEPARTMENTS)).thenReturn(10L);
    }

    private static Department department(String id, String name) throws Exception {
        var ctor = Department.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        Department department = ctor.newInstance();
        setField(department, "id", id);
        setField(department, "name", name);
        return department;
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static RunProblemView p(String rule, String goal, String severity) {
        return new RunProblemView(rule, goal, severity);
    }

    private void problems(String runId, RunProblemView... views) {
        when(runProblemRepository.findByRunAndDepartments(runId, DEPARTMENTS)).thenReturn(List.of(views));
    }

    private static Metric metric(KkdSummary summary, String key) {
        return summary.metrics().stream().filter(m -> m.key().equals(key)).findFirst().orElseThrow();
    }

    @Test
    void returnsRunUnitAndComparedRun() {
        problems("RUN-008");
        problems("RUN-007");

        KkdSummary summary = service.getSummary("RUN-008", "HQ-CIB");

        assertThat(summary.run().id()).isEqualTo("RUN-008");
        assertThat(summary.run().snapshotDate()).isEqualTo(CURRENT_DATE);
        assertThat(summary.comparedTo().id()).isEqualTo("RUN-007");
        assertThat(summary.comparedTo().snapshotDate()).isEqualTo(PREVIOUS_DATE);
        assertThat(summary.unit().id()).isEqualTo("HQ-CIB");
        assertThat(summary.unit().name()).isEqualTo("Департамент");
    }

    @Test
    void returnsThreeMetricsInFixedOrder() {
        problems("RUN-008");
        problems("RUN-007");

        KkdSummary summary = service.getSummary("RUN-008", "HQ-CIB");

        assertThat(summary.metrics()).extracting(Metric::key)
                .containsExactly("clean_goals_share", "issues_per_goal", "critical_goals_share");
    }

    @Test
    void worseningCleanShareIsWorseAndGrowthOfIssuesIsWorse() {
        // текущий: цели g1,g2,g3 с проблемами (critical у g1) -> clean 70, per goal 0.4, critical 10
        problems("RUN-008",
                p("r1", "g1", "critical"), p("r2", "g2", "major"), p("r3", "g3", "major"), p("r4", "g3", "major"));
        // предыдущий: одна цель с проблемой -> clean 90, per goal 0.1, critical 0
        problems("RUN-007", p("r2", "g2", "major"));

        KkdSummary summary = service.getSummary("RUN-008", "HQ-CIB");

        Metric clean = metric(summary, "clean_goals_share");
        assertThat(clean.value()).isEqualByComparingTo("70");
        assertThat(clean.previous()).isEqualByComparingTo("90");
        assertThat(clean.delta()).isEqualByComparingTo("-20");
        assertThat(clean.trend()).isEqualTo(Trend.WORSE);

        Metric perGoal = metric(summary, "issues_per_goal");
        assertThat(perGoal.value()).isEqualByComparingTo("0.4");
        assertThat(perGoal.delta()).isEqualByComparingTo("0.3");
        assertThat(perGoal.trend()).isEqualTo(Trend.WORSE);

        Metric critical = metric(summary, "critical_goals_share");
        assertThat(critical.delta()).isEqualByComparingTo("10");
        assertThat(critical.trend()).isEqualTo(Trend.WORSE);
    }

    @Test
    void improvementIsBetterAndEqualIsSame() {
        problems("RUN-008", p("r2", "g2", "major"));
        problems("RUN-007", p("r1", "g1", "critical"), p("r2", "g2", "major"));

        KkdSummary summary = service.getSummary("RUN-008", "HQ-CIB");

        assertThat(metric(summary, "clean_goals_share").trend()).isEqualTo(Trend.BETTER);
        assertThat(metric(summary, "issues_per_goal").trend()).isEqualTo(Trend.BETTER);
        assertThat(metric(summary, "critical_goals_share").trend()).isEqualTo(Trend.BETTER);

        problems("RUN-008", p("r2", "g2", "major"));
        problems("RUN-007", p("r2", "g2", "major"));
        KkdSummary same = service.getSummary("RUN-008", "HQ-CIB");
        assertThat(same.metrics()).allSatisfy(m -> {
            assertThat(m.trend()).isEqualTo(Trend.SAME);
            assertThat(m.delta()).isEqualByComparingTo("0");
        });
    }

    @Test
    void withoutPreviousRunPreviousAndDeltaAreNull() {
        when(runRepository.findFirstBySnapshotDateLessThanOrderBySnapshotDateDescIdDesc(CURRENT_DATE))
                .thenReturn(Optional.empty());
        problems("RUN-008", p("r1", "g1", "critical"), p("r2", "g2", "major"), p("r3", "g3", "major"));

        KkdSummary summary = service.getSummary("RUN-008", "HQ-CIB");

        assertThat(summary.comparedTo()).isNull();
        assertThat(summary.metrics()).allSatisfy(m -> {
            assertThat(m.previous()).isNull();
            assertThat(m.delta()).isNull();
            assertThat(m.trend()).isEqualTo(Trend.SAME);
        });
        assertThat(summary.issuesBySeverity()).allSatisfy(s -> {
            assertThat(s.previous()).isNull();
            assertThat(s.delta()).isNull();
        });
        assertThat(summary.delta().created()).isEqualTo(3);
        assertThat(summary.delta().persisting()).isZero();
        assertThat(summary.delta().resolved()).isZero();
        verify(runProblemRepository, never()).findByRunAndDepartments(eq("RUN-007"), any());
    }

    @Test
    void groupsIssuesBySeverityWithZerosAndOrdersByCriticality() {
        problems("RUN-008",
                p("r1", "g1", "critical"), p("r2", "g2", "major"), p("r2", "g3", "major"), p("r5", "g4", "minor"));
        problems("RUN-007", p("r1", "g1", "critical"), p("r9", "g5", "info"), p("r9", "g6", "info"));

        KkdSummary summary = service.getSummary("RUN-008", "HQ-CIB");

        assertThat(summary.issuesBySeverity()).extracting(s -> s.code())
                .containsExactly("critical", "major", "info", "minor");
        var major = summary.issuesBySeverity().get(1);
        assertThat(major.current()).isEqualTo(2);
        assertThat(major.previous()).isZero();
        assertThat(major.delta()).isEqualTo(2);
        var info = summary.issuesBySeverity().get(2);
        assertThat(info.current()).isZero();
        assertThat(info.previous()).isEqualTo(2);
        assertThat(info.delta()).isEqualTo(-2);
    }

    @Test
    void calculatesNewPersistingAndResolvedByRuleAndGoalPair() {
        problems("RUN-008", p("r1", "g1", "critical"), p("r2", "g2", "major"), p("r3", "g3", "major"));
        problems("RUN-007", p("r1", "g1", "critical"), p("r2", "g9", "major"));

        KkdSummary summary = service.getSummary("RUN-008", "HQ-CIB");

        assertThat(summary.delta().created()).isEqualTo(2);
        assertThat(summary.delta().persisting()).isEqualTo(1);
        assertThat(summary.delta().resolved()).isEqualTo(1);
    }

    @Test
    void zeroGoalsGivesZeroMetricsAndStillWorks() {
        when(goalRepository.countByDepartmentIds(DEPARTMENTS)).thenReturn(0L);
        problems("RUN-008");
        problems("RUN-007");

        KkdSummary summary = service.getSummary("RUN-008", "HQ-CIB");

        assertThat(summary.metrics()).allSatisfy(m -> {
            assertThat(m.value()).isEqualByComparingTo("0");
            assertThat(m.trend()).isEqualTo(Trend.SAME);
        });
        assertThat(summary.issuesBySeverity()).isEmpty();
        assertThat(summary.delta().created()).isZero();
    }

    @Test
    void unknownRunThrowsNotFoundWithMessage() {
        when(runRepository.findById("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSummary("NOPE", "HQ-CIB"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Прогон с ID NOPE не найден");
    }

    @Test
    void unknownUnitThrowsNotFoundWithMessage() {
        when(departmentRepository.findById("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSummary("RUN-008", "NOPE"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Подразделение с ID NOPE не найдено");
    }

    @Test
    void propagatesRepositoryFailure() {
        when(runProblemRepository.findByRunAndDepartments("RUN-008", DEPARTMENTS))
                .thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> service.getSummary("RUN-008", "HQ-CIB"))
                .isInstanceOf(IllegalStateException.class);
    }
}
