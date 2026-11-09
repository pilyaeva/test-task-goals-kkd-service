package com.example.testtask.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.testtask.infrastructure.repository.RunRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Проверка GET /actuator/health. БД отключена, чтобы тест не зависел от PostgreSQL.
 */
class HealthEndpointTest {

    private static final String NO_DB =
            "spring.autoconfigure.exclude="
                    + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                    + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                    + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration,"
                    + "org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration";

    @Nested
    @SpringBootTest(properties = NO_DB)
    @AutoConfigureMockMvc
    class WhenHealthy {

        @Autowired
        MockMvc mockMvc;

        // JPA отключён, поэтому репозиторий, нужный RunService, подменяем
        @MockitoBean
        RunRepository runRepository;

        @MockitoBean
        com.example.testtask.infrastructure.repository.DepartmentRepository departmentRepository;

        @MockitoBean
        com.example.testtask.infrastructure.repository.GoalRepository goalRepository;

        @MockitoBean
        com.example.testtask.infrastructure.repository.RunProblemRepository runProblemRepository;

        @Test
        void returnsUpWithoutDetails() throws Exception {
            mockMvc.perform(get("/actuator/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.components").doesNotExist());
        }

        @Test
        void doesNotExposeOtherActuatorEndpoints() throws Exception {
            mockMvc.perform(get("/actuator/env"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @SpringBootTest(properties = NO_DB)
    @AutoConfigureMockMvc
    @Import(WhenDown.DownConfig.class)
    class WhenDown {

        @Autowired
        MockMvc mockMvc;

        @MockitoBean
        RunRepository runRepository;

        @MockitoBean
        com.example.testtask.infrastructure.repository.DepartmentRepository departmentRepository;

        @MockitoBean
        com.example.testtask.infrastructure.repository.GoalRepository goalRepository;

        @MockitoBean
        com.example.testtask.infrastructure.repository.RunProblemRepository runProblemRepository;

        @Test
        void returnsServiceUnavailableWithDownStatus() throws Exception {
            mockMvc.perform(get("/actuator/health"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.status").value("DOWN"))
                    .andExpect(jsonPath("$.components").doesNotExist());
        }

        @TestConfiguration
        static class DownConfig {
            @Bean
            HealthIndicator brokenIndicator() {
                return () -> Health.down().build();
            }
        }
    }
}
