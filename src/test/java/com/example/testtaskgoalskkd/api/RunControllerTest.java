package com.example.testtaskgoalskkd.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.testtaskgoalskkd.application.RunListItem;
import com.example.testtaskgoalskkd.application.RunService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RunController.class)
class RunControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    RunService runService;

    @Test
    void returnsRunsInSpecFormat() throws Exception {
        when(runService.getRuns()).thenReturn(List.of(
                new RunListItem("RUN-002", "COMPLETED", LocalDate.parse("2026-11-01"), "RUN-001", 5),
                new RunListItem("RUN-001", "COMPLETED", LocalDate.parse("2026-10-01"), null, 3)));

        mockMvc.perform(get("/api/runs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("RUN-002"))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$[0].snapshot_date").value("2026-11-01"))
                .andExpect(jsonPath("$[0].compared_run_id").value("RUN-001"))
                .andExpect(jsonPath("$[0].problems_count").value(5))
                .andExpect(jsonPath("$[1].compared_run_id").hasJsonPath())
                .andExpect(jsonPath("$[1].compared_run_id").isEmpty())
                .andExpect(jsonPath("$[1].problems_count").value(3));
    }

    @Test
    void returnsEmptyArrayWhenNoRuns() throws Exception {
        when(runService.getRuns()).thenReturn(List.of());

        mockMvc.perform(get("/api/runs"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]", true));
    }

    @Test
    void passesStatusThroughUnchanged() throws Exception {
        when(runService.getRuns()).thenReturn(List.of(
                new RunListItem("RUN-001", "FAILED", LocalDate.parse("2026-01-01"), null, 0)));

        mockMvc.perform(get("/api/runs"))
                .andExpect(jsonPath("$[0].status").value("FAILED"))
                .andExpect(jsonPath("$[0].problems_count").value(0));
    }

    @Test
    void startRunReturns201WithCreatedRun() throws Exception {
        when(runService.startRun()).thenReturn(
                new RunListItem("RUN-003", "COMPLETED", LocalDate.parse("2026-11-09"), "RUN-002", 9));

        mockMvc.perform(post("/api/runs"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("RUN-003"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.snapshot_date").value("2026-11-09"))
                .andExpect(jsonPath("$.compared_run_id").value("RUN-002"))
                .andExpect(jsonPath("$.problems_count").value(9));
    }

    @Test
    void startRunFirstRunHasNoComparison() throws Exception {
        when(runService.startRun()).thenReturn(
                new RunListItem("RUN-001", "COMPLETED", LocalDate.parse("2026-11-09"), null, 0));

        mockMvc.perform(post("/api/runs"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.compared_run_id").isEmpty())
                .andExpect(jsonPath("$.problems_count").value(0));
    }

    @Test
    void startRunIgnoresRequestBody() throws Exception {
        when(runService.startRun()).thenReturn(
                new RunListItem("RUN-001", "COMPLETED", LocalDate.parse("2026-11-09"), null, 0));

        mockMvc.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"snapshot_date\": \"2000-01-01\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.snapshot_date").value("2026-11-09"));
    }

    @Test
    void startRunFailureReturns500WithoutLeakingDetails() throws Exception {
        when(runService.startRun()).thenThrow(new IllegalStateException("insert into runs failed"));

        mockMvc.perform(post("/api/runs"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(content().string(not(containsString("insert"))));
    }

    @Test
    void unexpectedFailureReturns500WithoutLeakingDetails() throws Exception {
        when(runService.getRuns()).thenThrow(new IllegalStateException("SELECT * FROM runs failed"));

        mockMvc.perform(get("/api/runs"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.error.message").value("Внутренняя ошибка сервера"))
                .andExpect(content().string(not(containsString("SELECT"))))
                .andExpect(content().string(not(containsString("IllegalState"))));
    }
}
