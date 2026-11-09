package com.example.testtaskgoalskkd.infrastructure.repository;

import com.example.testtaskgoalskkd.domain.Goal;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GoalRepository extends JpaRepository<Goal, String> {

    /**
     * Цели с указанным статусом.
     */
    List<Goal> findByStatusId(String statusId);

    /**
     * Число целей, исполнители которых работают в указанных подразделениях.
     */
    @Query("""
            select count(g) from Goal g
            join Employee e on e.id = g.assigneeEmployeeId
            where e.departmentId in :departmentIds
            """)
    long countByDepartmentIds(@Param("departmentIds") Collection<String> departmentIds);
}
