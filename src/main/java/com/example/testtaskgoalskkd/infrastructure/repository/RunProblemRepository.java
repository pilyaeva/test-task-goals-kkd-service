package com.example.testtaskgoalskkd.infrastructure.repository;

import com.example.testtaskgoalskkd.domain.RunProblem;
import com.example.testtaskgoalskkd.domain.RunProblemView;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RunProblemRepository extends JpaRepository<RunProblem, String> {

    /**
     * Проблемы прогона по целям, исполнители которых работают в указанных подразделениях,
     * с критичностью правила.
     */
    @Query("""
            select new com.example.testtaskgoalskkd.domain.RunProblemView(p.ruleId, p.goalId, r.severity)
            from RunProblem p
            join Rule r on r.id = p.ruleId
            join Goal g on g.id = p.goalId
            join Employee e on e.id = g.assigneeEmployeeId
            where p.runId = :runId and e.departmentId in :departmentIds
            """)
    List<RunProblemView> findByRunAndDepartments(
            @Param("runId") String runId, @Param("departmentIds") Collection<String> departmentIds);
}
