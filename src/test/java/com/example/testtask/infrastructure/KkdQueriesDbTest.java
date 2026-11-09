package com.example.testtask.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.testtask.domain.RunProblemView;
import com.example.testtask.infrastructure.repository.DepartmentRepository;
import com.example.testtask.infrastructure.repository.GoalRepository;
import com.example.testtask.infrastructure.repository.RunProblemRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Запросы для GET /api/kkd/summary на реальном PostgreSQL 18: дерево подразделений,
 * цели подразделения и проблемы прогона. HTTP не используется.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class KkdQueriesDbTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @Autowired
    DepartmentRepository departmentRepository;
    @Autowired
    GoalRepository goalRepository;
    @Autowired
    RunProblemRepository runProblemRepository;
    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        // HQ -> CIB -> CIB-1; соседний корень OTHER
        jdbc.update("INSERT INTO departments (id, parent_department_id, name) VALUES ('HQ', NULL, 'HQ')");
        jdbc.update("INSERT INTO departments (id, parent_department_id, name) VALUES ('CIB', 'HQ', 'CIB')");
        jdbc.update("INSERT INTO departments (id, parent_department_id, name) VALUES ('CIB-1', 'CIB', 'CIB-1')");
        jdbc.update("INSERT INTO departments (id, parent_department_id, name) VALUES ('OTHER', NULL, 'OTHER')");
        jdbc.update("INSERT INTO employees (id, department_id, name) VALUES ('E-HQ', 'HQ', 'a')");
        jdbc.update("INSERT INTO employees (id, department_id, name) VALUES ('E-CIB', 'CIB', 'b')");
        jdbc.update("INSERT INTO employees (id, department_id, name) VALUES ('E-CIB1', 'CIB-1', 'c')");
        jdbc.update("INSERT INTO employees (id, department_id, name) VALUES ('E-OTHER', 'OTHER', 'd')");
        jdbc.update("INSERT INTO goal_statuses (id, name) VALUES ('ST', 's')");
        jdbc.update("INSERT INTO goals (id, assignee_employee_id, status_id) VALUES ('G-HQ', 'E-HQ', 'ST')");
        jdbc.update("INSERT INTO goals (id, assignee_employee_id, status_id) VALUES ('G-CIB', 'E-CIB', 'ST')");
        jdbc.update("INSERT INTO goals (id, assignee_employee_id, status_id) VALUES ('G-CIB1', 'E-CIB1', 'ST')");
        jdbc.update("INSERT INTO goals (id, assignee_employee_id, status_id) VALUES ('G-OTHER', 'E-OTHER', 'ST')");
        jdbc.update("INSERT INTO goals (id, assignee_employee_id, status_id) VALUES ('G-NOOWNER', NULL, 'ST')");
        jdbc.update("INSERT INTO rules (id, code, severity) VALUES ('R1', 'GOAL_NO_OWNER', 'critical')");
        jdbc.update("INSERT INTO rules (id, code, severity) VALUES ('R2', 'KR_NO_WEIGHT', 'major')");
        jdbc.update("INSERT INTO runs (id, status, snapshot_date) VALUES ('RUN-1', 'COMPLETED', '2026-10-19')");
        jdbc.update("INSERT INTO runs (id, status, snapshot_date) VALUES ('RUN-2', 'COMPLETED', '2026-10-26')");
        problem("P1", "RUN-1", "R1", "G-CIB");
        problem("P2", "RUN-2", "R1", "G-CIB");
        problem("P3", "RUN-2", "R2", "G-CIB1");
        problem("P4", "RUN-2", "R2", "G-HQ");
        problem("P5", "RUN-2", "R2", "G-OTHER");
        problem("P6", "RUN-2", "R2", "G-NOOWNER");
    }

    private void problem(String id, String run, String rule, String goal) {
        jdbc.update("INSERT INTO run_problems (id, run_id, rule_id, goal_id) VALUES (?, ?, ?, ?)",
                id, run, rule, goal);
    }

    @Test
    void subtreeContainsDepartmentAndAllDescendants() {
        assertThat(departmentRepository.findSubtreeIds("HQ")).containsExactlyInAnyOrder("HQ", "CIB", "CIB-1");
        assertThat(departmentRepository.findSubtreeIds("CIB")).containsExactlyInAnyOrder("CIB", "CIB-1");
        assertThat(departmentRepository.findSubtreeIds("CIB-1")).containsExactly("CIB-1");
    }

    @Test
    void subtreeOfUnknownDepartmentIsEmpty() {
        assertThat(departmentRepository.findSubtreeIds("NOPE")).isEmpty();
    }

    @Test
    void countsGoalsOfDepartmentsOnly() {
        assertThat(goalRepository.countByDepartmentIds(List.of("HQ", "CIB", "CIB-1"))).isEqualTo(3);
        assertThat(goalRepository.countByDepartmentIds(List.of("CIB", "CIB-1"))).isEqualTo(2);
        assertThat(goalRepository.countByDepartmentIds(List.of("NOPE"))).isZero();
    }

    @Test
    void returnsProblemsOfRunForDepartmentGoalsWithSeverity() {
        List<RunProblemView> result =
                runProblemRepository.findByRunAndDepartments("RUN-2", List.of("CIB", "CIB-1"));

        assertThat(result).containsExactlyInAnyOrder(
                new RunProblemView("R1", "G-CIB", "critical"),
                new RunProblemView("R2", "G-CIB1", "major"));
    }

    @Test
    void doesNotMixProblemsOfOtherRuns() {
        assertThat(runProblemRepository.findByRunAndDepartments("RUN-1", List.of("CIB", "CIB-1")))
                .containsExactly(new RunProblemView("R1", "G-CIB", "critical"));
        assertThat(runProblemRepository.findByRunAndDepartments("NOPE", List.of("CIB"))).isEmpty();
    }
}
