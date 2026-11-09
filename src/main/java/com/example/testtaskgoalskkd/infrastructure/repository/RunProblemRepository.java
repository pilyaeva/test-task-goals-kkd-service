package com.example.testtaskgoalskkd.infrastructure.repository;

import com.example.testtaskgoalskkd.domain.IssueView;
import com.example.testtaskgoalskkd.domain.RunProblem;
import com.example.testtaskgoalskkd.domain.RunProblemView;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RunProblemRepository extends JpaRepository<RunProblem, String> {

    /**
     * Идентификаторы всех проблем (для генерации следующего номера).
     */
    @Query("select p.id from RunProblem p")
    List<String> findAllIds();

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

    /**
     * Проблемы прогона с необязательными фильтрами, от более критичных к менее, затем по id.
     * Если {@code allUnits} = true, фильтр по подразделениям не применяется (в {@code unitIds}
     * тогда передаётся любой непустой список); проблемы целей без исполнителя
     * в выборку по подразделениям не попадают.
     */
    @Query(value = """
            select new com.example.testtaskgoalskkd.domain.IssueView(
                p.id, r.code, r.severity, p.goalId, d.id, d.name)
            from RunProblem p
            join Rule r on r.id = p.ruleId
            join Goal g on g.id = p.goalId
            left join Employee e on e.id = g.assigneeEmployeeId
            left join Department d on d.id = e.departmentId
            where p.runId = :runId
              and (:allUnits = true or d.id in :unitIds)
              and (cast(:ruleCode as string) is null or r.code = :ruleCode)
              and (cast(:severity as string) is null or r.severity = :severity)
            order by case r.severity when 'critical' then 0 when 'major' then 1 when 'minor' then 2 else 3 end,
                     p.id
            """,
            countQuery = """
            select count(p)
            from RunProblem p
            join Rule r on r.id = p.ruleId
            join Goal g on g.id = p.goalId
            left join Employee e on e.id = g.assigneeEmployeeId
            left join Department d on d.id = e.departmentId
            where p.runId = :runId
              and (:allUnits = true or d.id in :unitIds)
              and (cast(:ruleCode as string) is null or r.code = :ruleCode)
              and (cast(:severity as string) is null or r.severity = :severity)
            """)
    Page<IssueView> findIssues(
            @Param("runId") String runId,
            @Param("allUnits") boolean allUnits,
            @Param("unitIds") Collection<String> unitIds,
            @Param("ruleCode") String ruleCode,
            @Param("severity") String severity,
            Pageable pageable);
}
