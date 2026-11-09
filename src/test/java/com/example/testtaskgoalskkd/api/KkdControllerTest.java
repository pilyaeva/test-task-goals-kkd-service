package com.example.testtaskgoalskkd.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.testtaskgoalskkd.application.IssuesPage;
import com.example.testtaskgoalskkd.application.IssuesService;
import com.example.testtaskgoalskkd.application.KkdSummary;
import com.example.testtaskgoalskkd.application.KkdSummary.Metric;
import com.example.testtaskgoalskkd.application.KkdSummary.ProblemsDelta;
import com.example.testtaskgoalskkd.application.KkdSummary.RunRef;
import com.example.testtaskgoalskkd.application.KkdSummary.SeverityCount;
import com.example.testtaskgoalskkd.application.KkdSummary.Trend;
import com.example.testtaskgoalskkd.application.KkdSummary.UnitRef;
import com.example.testtaskgoalskkd.application.KkdSummaryService;
import com.example.testtaskgoalskkd.application.NotFoundException;
import com.example.testtaskgoalskkd.application.ValidationException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(KkdController.class)
class KkdControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    KkdSummaryService service;

    @MockitoBean
    IssuesService issuesService;

    private static KkdSummary summary(RunRef comparedTo) {
        boolean hasPrevious = comparedTo != null;
        return new KkdSummary(
                new RunRef("RUN-008", LocalDate.parse("2026-10-26")),
                comparedTo,
                new UnitRef("HQ-CIB", "Департамент корпоративного бизнеса"),
                List.of(new Metric("clean_goals_share", "Доля корректных целей", "%",
                        new BigDecimal("56.5"),
                        hasPrevious ? new BigDecimal("62.7") : null,
                        hasPrevious ? new BigDecimal("-6.2") : null,
                        hasPrevious ? Trend.WORSE : Trend.SAME)),
                List.of(new SeverityCount("critical", 2, hasPrevious ? 2 : null, hasPrevious ? 0 : null),
                        new SeverityCount("major", 23, hasPrevious ? 15 : null, hasPrevious ? 8 : null)),
                new ProblemsDelta(10, 33, 2));
    }

    @Test
    void returnsSummaryInSpecFormat() throws Exception {
        when(service.getSummary("RUN-008", "HQ-CIB"))
                .thenReturn(summary(new RunRef("RUN-007", LocalDate.parse("2026-10-19"))));

        mockMvc.perform(get("/api/kkd/summary").param("run_id", "RUN-008").param("unit_id", "HQ-CIB"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.run.id").value("RUN-008"))
                .andExpect(jsonPath("$.run.snapshot_date").value("2026-10-26"))
                .andExpect(jsonPath("$.compared_to.id").value("RUN-007"))
                .andExpect(jsonPath("$.compared_to.snapshot_date").value("2026-10-19"))
                .andExpect(jsonPath("$.unit.id").value("HQ-CIB"))
                .andExpect(jsonPath("$.unit.name").value("Департамент корпоративного бизнеса"))
                .andExpect(jsonPath("$.metrics[0].key").value("clean_goals_share"))
                .andExpect(jsonPath("$.metrics[0].title").value("Доля корректных целей"))
                .andExpect(jsonPath("$.metrics[0].unit").value("%"))
                .andExpect(jsonPath("$.metrics[0].value").value(56.5))
                .andExpect(jsonPath("$.metrics[0].previous").value(62.7))
                .andExpect(jsonPath("$.metrics[0].delta").value(-6.2))
                .andExpect(jsonPath("$.metrics[0].trend").value("worse"))
                .andExpect(jsonPath("$.issues_by_severity[1].code").value("major"))
                .andExpect(jsonPath("$.issues_by_severity[1].current").value(23))
                .andExpect(jsonPath("$.issues_by_severity[1].previous").value(15))
                .andExpect(jsonPath("$.issues_by_severity[1].delta").value(8))
                .andExpect(jsonPath("$.delta.new").value(10))
                .andExpect(jsonPath("$.delta.persisting").value(33))
                .andExpect(jsonPath("$.delta.resolved").value(2));
    }

    @Test
    void nullsArePresentExplicitlyWhenNoPreviousRun() throws Exception {
        when(service.getSummary("RUN-008", "HQ-CIB")).thenReturn(summary(null));

        mockMvc.perform(get("/api/kkd/summary").param("run_id", "RUN-008").param("unit_id", "HQ-CIB"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.compared_to").hasJsonPath())
                .andExpect(jsonPath("$.compared_to").isEmpty())
                .andExpect(jsonPath("$.metrics[0].previous").hasJsonPath())
                .andExpect(jsonPath("$.metrics[0].previous").isEmpty())
                .andExpect(jsonPath("$.metrics[0].delta").isEmpty())
                .andExpect(jsonPath("$.issues_by_severity[0].previous").isEmpty())
                .andExpect(jsonPath("$.issues_by_severity[0].delta").isEmpty());
    }

    @Test
    void missingRunIdReturns422InUnifiedFormat() throws Exception {
        mockMvc.perform(get("/api/kkd/summary").param("unit_id", "HQ-CIB"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message").value(containsString("run_id")));
    }

    @Test
    void missingUnitIdReturns422InUnifiedFormat() throws Exception {
        mockMvc.perform(get("/api/kkd/summary").param("run_id", "RUN-008"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message").value(containsString("unit_id")));
    }

    @Test
    void unknownRunReturns404WithMessage() throws Exception {
        when(service.getSummary("X", "HQ-CIB")).thenThrow(new NotFoundException("Прогон с ID X не найден"));

        mockMvc.perform(get("/api/kkd/summary").param("run_id", "X").param("unit_id", "HQ-CIB"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("Прогон с ID X не найден"));
    }

    @Test
    void unknownUnitReturns404WithMessage() throws Exception {
        when(service.getSummary("RUN-008", "X")).thenThrow(new NotFoundException("Подразделение с ID X не найдено"));

        mockMvc.perform(get("/api/kkd/summary").param("run_id", "RUN-008").param("unit_id", "X"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.message").value("Подразделение с ID X не найдено"));
    }

    @Test
    void missingBothParametersReturns422() throws Exception {
        mockMvc.perform(get("/api/kkd/summary"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(service);
    }

    // --- GET /api/kkd/issues ---

    @Test
    void issuesReturnsPageInSpecFormat() throws Exception {
        when(issuesService.getIssues("RUN-002", "HQ-CIB", null, "critical", 2, 0L)).thenReturn(new IssuesPage(
                List.of(
                        new IssuesPage.Issue("PRB-002", "GOAL_NO_OWNER", "critical", "G-05", null),
                        new IssuesPage.Issue("PRB-001", "GOAL_NO_KR", "critical", "G-01",
                                new IssuesPage.UnitRef("CIB-SALES", "Отдел продаж КИБ"))),
                7, 2, 0));

        mockMvc.perform(get("/api/kkd/issues")
                        .param("run_id", "RUN-002").param("unit_id", "HQ-CIB").param("severity", "critical")
                        .param("limit", "2").param("offset", "0"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items[0].id").value("PRB-002"))
                .andExpect(jsonPath("$.items[0].rule_code").value("GOAL_NO_OWNER"))
                .andExpect(jsonPath("$.items[0].severity").value("critical"))
                .andExpect(jsonPath("$.items[0].goal_id").value("G-05"))
                .andExpect(jsonPath("$.items[0].unit").hasJsonPath())
                .andExpect(jsonPath("$.items[0].unit").isEmpty())
                .andExpect(jsonPath("$.items[1].unit.id").value("CIB-SALES"))
                .andExpect(jsonPath("$.items[1].unit.name").value("Отдел продаж КИБ"))
                .andExpect(jsonPath("$.total").value(7))
                .andExpect(jsonPath("$.limit").value(2))
                .andExpect(jsonPath("$.offset").value(0));
    }

    @Test
    void issuesPassesOnlyRunIdWhenNoOptionalParameters() throws Exception {
        when(issuesService.getIssues("RUN-002", null, null, null, null, null))
                .thenReturn(new IssuesPage(List.of(), 0, 20, 0));

        mockMvc.perform(get("/api/kkd/issues").param("run_id", "RUN-002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.limit").value(20));
        verify(issuesService).getIssues("RUN-002", null, null, null, null, null);
    }

    @Test
    void issuesWithoutRunIdReturns422() throws Exception {
        mockMvc.perform(get("/api/kkd/issues"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(issuesService);
    }

    @Test
    void issuesWithNonNumericLimitOrOffsetReturns422() throws Exception {
        mockMvc.perform(get("/api/kkd/issues").param("run_id", "RUN-002").param("limit", "abc"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        mockMvc.perform(get("/api/kkd/issues").param("run_id", "RUN-002").param("offset", "abc"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(issuesService);
    }

    @Test
    void issuesValidationFailureOfServiceReturns422() throws Exception {
        when(issuesService.getIssues("RUN-002", null, null, null, 0, null))
                .thenThrow(new ValidationException("Параметр limit должен быть от 1 до 100"));

        mockMvc.perform(get("/api/kkd/issues").param("run_id", "RUN-002").param("limit", "0"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message").value("Параметр limit должен быть от 1 до 100"));
    }

    @Test
    void issuesNotFoundReturns404WithMessage() throws Exception {
        when(issuesService.getIssues("RUN-999", null, null, null, null, null))
                .thenThrow(new NotFoundException("Прогон с ID RUN-999 не найден"));

        mockMvc.perform(get("/api/kkd/issues").param("run_id", "RUN-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("Прогон с ID RUN-999 не найден"));
    }

    @Test
    void issuesUnexpectedFailureReturns500WithoutLeakingDetails() throws Exception {
        when(issuesService.getIssues("RUN-002", null, null, null, null, null))
                .thenThrow(new IllegalStateException("SELECT secret"));

        mockMvc.perform(get("/api/kkd/issues").param("run_id", "RUN-002"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(content().string(not(containsString("SELECT"))));
    }

    @Test
    void unexpectedFailureReturns500WithoutLeakingDetails() throws Exception {
        when(service.getSummary("RUN-008", "HQ-CIB")).thenThrow(new IllegalStateException("SELECT secret"));

        mockMvc.perform(get("/api/kkd/summary").param("run_id", "RUN-008").param("unit_id", "HQ-CIB"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(content().string(not(containsString("SELECT"))));
    }
}
