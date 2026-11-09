package com.example.testtaskgoalskkd.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.testtaskgoalskkd.domain.IssueView;
import com.example.testtaskgoalskkd.infrastructure.repository.DepartmentRepository;
import com.example.testtaskgoalskkd.infrastructure.repository.RuleRepository;
import com.example.testtaskgoalskkd.infrastructure.repository.RunProblemRepository;
import com.example.testtaskgoalskkd.infrastructure.repository.RunRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IssuesServiceTest {

    @Mock
    RunRepository runRepository;
    @Mock
    DepartmentRepository departmentRepository;
    @Mock
    RuleRepository ruleRepository;
    @Mock
    RunProblemRepository runProblemRepository;

    @InjectMocks
    IssuesServiceImpl service;

    @BeforeEach
    void setUp() {
        when(runRepository.existsById("RUN-002")).thenReturn(true);
        when(departmentRepository.existsById("U")).thenReturn(true);
        when(departmentRepository.findSubtreeIds("U")).thenReturn(List.of("U", "C"));
        when(ruleRepository.existsByCode("R-MAJ")).thenReturn(true);
        when(runProblemRepository.findIssues(any(), anyBoolean(), any(), any(), any(), any()))
                .thenReturn(Page.empty());
    }

    // 10
    @Test
    void usesDefaultLimitAndOffset() {
        IssuesPage page = service.getIssues("RUN-002", null, null, null, null, null);

        assertThat(page.limit()).isEqualTo(20);
        assertThat(page.offset()).isZero();
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(runProblemRepository).findIssues(eq("RUN-002"), eq(true), any(), eq(null), eq(null), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
        assertThat(pageable.getValue().getOffset()).isZero();
    }

    @Test
    void mapsProblemsAndTotalAndKeepsUnitNullForGoalWithoutAssignee() {
        when(runProblemRepository.findIssues(any(), anyBoolean(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(
                        List.of(
                                new IssueView("PRB-002", "GOAL_NO_OWNER", "critical", "G-05", null, null),
                                new IssueView("PRB-001", "GOAL_NO_KR", "critical", "G-01", "CIB-SALES", "Отдел продаж")),
                        PageRequest.of(0, 2),
                        7));

        IssuesPage page = service.getIssues("RUN-002", null, null, null, 2, 0L);

        assertThat(page.total()).isEqualTo(7);
        assertThat(page.items().get(0).unit()).isNull();
        assertThat(page.items().get(1).unit()).isEqualTo(new IssuesPage.UnitRef("CIB-SALES", "Отдел продаж"));
        assertThat(page.items().get(1).ruleCode()).isEqualTo("GOAL_NO_KR");
    }

    @Test
    void passesFiltersAndSubtreeOfUnitToRepository() {
        service.getIssues("RUN-002", "U", "R-MAJ", "major", 10, 5L);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(runProblemRepository).findIssues(
                eq("RUN-002"), eq(false), eq(List.of("U", "C")), eq("R-MAJ"), eq("major"), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
        assertThat(pageable.getValue().getOffset()).isEqualTo(5);
    }

    // 11
    @ParameterizedTest
    @ValueSource(ints = {1, 100})
    void acceptsBoundaryLimits(int limit) {
        assertThat(service.getIssues("RUN-002", null, null, null, limit, null).limit()).isEqualTo(limit);
    }

    // 18
    @Test
    void unknownRunThrowsNotFound() {
        assertThatThrownBy(() -> service.getIssues("RUN-999", null, null, null, null, null))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Прогон с ID RUN-999 не найден");
    }

    // 19
    @Test
    void unknownUnitThrowsNotFound() {
        assertThatThrownBy(() -> service.getIssues("RUN-002", "UNKNOWN", null, null, null, null))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Подразделение с ID UNKNOWN не найдено");
    }

    // 20
    @Test
    void unknownRuleThrowsNotFound() {
        assertThatThrownBy(() -> service.getIssues("RUN-002", null, "UNKNOWN", null, null, null))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Правило с кодом UNKNOWN не найдено");
    }

    // 21
    @Test
    void runIsCheckedBeforeUnit() {
        assertThatThrownBy(() -> service.getIssues("RUN-999", "UNKNOWN", null, null, null, null))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Прогон с ID RUN-999 не найден");
    }

    // 22
    @Test
    void missingOrBlankRunIdIsValidationErrorWithoutDbAccess() {
        assertThatThrownBy(() -> service.getIssues(null, null, null, null, null, null))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.getIssues("  ", null, null, null, null, null))
                .isInstanceOf(ValidationException.class);

        verifyNoInteractions(runRepository, departmentRepository, ruleRepository, runProblemRepository);
    }

    // 23
    @ParameterizedTest
    @ValueSource(ints = {0, 101, -5})
    void limitOutOfRangeIsValidationError(int limit) {
        assertThatThrownBy(() -> service.getIssues("RUN-002", null, null, null, limit, null))
                .isInstanceOf(ValidationException.class);
        verifyNoInteractions(runProblemRepository);
    }

    // 24
    @Test
    void negativeOffsetIsValidationError() {
        assertThatThrownBy(() -> service.getIssues("RUN-002", null, null, null, null, -1L))
                .isInstanceOf(ValidationException.class);
        verifyNoInteractions(runProblemRepository);
    }

    // 25
    @ParameterizedTest
    @ValueSource(strings = {"blocker", "CRITICAL", ""})
    void unknownSeverityIsValidationError(String severity) {
        assertThatThrownBy(() -> service.getIssues("RUN-002", null, null, severity, null, null))
                .isInstanceOf(ValidationException.class);
        verifyNoInteractions(runProblemRepository);
    }

    // 26
    @Test
    void propagatesRepositoryFailure() {
        when(runProblemRepository.findIssues(any(), anyBoolean(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> service.getIssues("RUN-002", null, null, null, null, null))
                .isInstanceOf(IllegalStateException.class);
    }
}
