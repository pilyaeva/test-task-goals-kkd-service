package com.example.testtaskgoalskkd.infrastructure.repository;

import com.example.testtaskgoalskkd.domain.Department;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DepartmentRepository extends JpaRepository<Department, String> {

    /**
     * Идентификаторы подразделения и всех его вложенных подразделений (рекурсивно).
     */
    @Query(value = """
            WITH RECURSIVE tree AS (
                SELECT id FROM departments WHERE id = :departmentId
                UNION
                SELECT d.id FROM departments d JOIN tree t ON d.parent_department_id = t.id
            )
            SELECT id FROM tree
            """, nativeQuery = true)
    List<String> findSubtreeIds(@Param("departmentId") String departmentId);
}
