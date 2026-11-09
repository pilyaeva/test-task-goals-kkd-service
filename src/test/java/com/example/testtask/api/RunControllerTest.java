package com.example.testtask.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.testtask.application.RunListItem;
import com.example.testtask.application.RunService;
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
                new RunListItem(2L, "COMPLETED", LocalDate.parse("2026-11-01"), 1L, 5),
                new RunListItem(1L, "COMPLETED", LocalDate.parse("2026-10-01"), null, 3)));

        mockMvc.perform(get("/api/runs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$[0].snapshotDate").value("2026-11-01"))
                .andExpect(jsonPath("$[0].comparedRunId").value(1))
                .andExpect(jsonPath("$[0].problemsCount").value(5))
                .andExpect(jsonPath("$[1].comparedRunId").hasJsonPath())
                .andExpect(jsonPath("$[1].comparedRunId").isEmpty())
                .andExpect(jsonPath("$[1].problemsCount").value(3));
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
                new RunListItem(1L, "FAILED", LocalDate.parse("2026-01-01"), null, 0)));

        mockMvc.perform(get("/api/runs"))
                .andExpect(jsonPath("$[0].status").value("FAILED"))
                .andExpect(jsonPath("$[0].problemsCount").value(0));
    }

    @Test
    void postIsNotAllowedInUnifiedErrorFormat() throws Exception {
        mockMvc.perform(post("/api/runs"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("GET")))
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void unexpectedFailureReturns500WithoutLeakingDetails() throws Exception {
        when(runService.getRuns()).thenThrow(new IllegalStateException("SELECT * FROM runs failed"));

        mockMvc.perform(get("/api/runs"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Внутренняя ошибка сервера"))
                .andExpect(content().string(not(containsString("SELECT"))))
                .andExpect(content().string(not(containsString("IllegalState"))));
    }

    @Test
    void unknownPathReturns404InUnifiedErrorFormat() throws Exception {
        mockMvc.perform(get("/api/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
