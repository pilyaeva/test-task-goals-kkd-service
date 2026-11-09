package com.example.testtask.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.testtask.application.KkdSummary;
import com.example.testtask.application.KkdSummary.Metric;
import com.example.testtask.application.KkdSummary.ProblemsDelta;
import com.example.testtask.application.KkdSummary.RunRef;
import com.example.testtask.application.KkdSummary.SeverityCount;
import com.example.testtask.application.KkdSummary.Trend;
import com.example.testtask.application.KkdSummary.UnitRef;
import com.example.testtask.application.KkdSummaryService;
import com.example.testtask.application.NotFoundException;
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
    void doesNotExposeCamelCaseFields() throws Exception {
        when(service.getSummary("RUN-008", "HQ-CIB")).thenReturn(summary(null));

        mockMvc.perform(get("/api/kkd/summary").param("run_id", "RUN-008").param("unit_id", "HQ-CIB"))
                .andExpect(content().string(not(containsString("snapshotDate"))))
                .andExpect(content().string(not(containsString("issuesBySeverity"))))
                .andExpect(content().string(not(containsString("comparedTo"))));
    }

    @Test
    void missingRunIdReturns400InUnifiedFormat() throws Exception {
        mockMvc.perform(get("/api/kkd/summary").param("unit_id", "HQ-CIB"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value(containsString("run_id")));
    }

    @Test
    void missingUnitIdReturns400InUnifiedFormat() throws Exception {
        mockMvc.perform(get("/api/kkd/summary").param("run_id", "RUN-008"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value(containsString("unit_id")));
    }

    @Test
    void unknownRunReturns404WithMessage() throws Exception {
        when(service.getSummary("X", "HQ-CIB")).thenThrow(new NotFoundException("Прогон с ID X не найден"));

        mockMvc.perform(get("/api/kkd/summary").param("run_id", "X").param("unit_id", "HQ-CIB"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Прогон с ID X не найден"));
    }

    @Test
    void unknownUnitReturns404WithMessage() throws Exception {
        when(service.getSummary("RUN-008", "X")).thenThrow(new NotFoundException("Подразделение с ID X не найдено"));

        mockMvc.perform(get("/api/kkd/summary").param("run_id", "RUN-008").param("unit_id", "X"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Подразделение с ID X не найдено"));
    }

    @Test
    void postIsNotAllowed() throws Exception {
        mockMvc.perform(post("/api/kkd/summary"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void unexpectedFailureReturns500WithoutLeakingDetails() throws Exception {
        when(service.getSummary("RUN-008", "HQ-CIB")).thenThrow(new IllegalStateException("SELECT secret"));

        mockMvc.perform(get("/api/kkd/summary").param("run_id", "RUN-008").param("unit_id", "HQ-CIB"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(content().string(not(containsString("SELECT"))));
    }
}
