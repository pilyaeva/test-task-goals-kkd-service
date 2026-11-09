package com.example.testtask.infrastructure.repository;

import com.example.testtask.domain.Run;
import com.example.testtask.domain.RunSummary;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RunRepository extends JpaRepository<Run, Long> {

    /**
     * Все прогоны с числом проблем одним запросом (без N+1),
     * от новых к старым; при равной дате снимка — по id по убыванию.
     */
    @Query("""
            select new com.example.testtask.domain.RunSummary(
                r.id, r.status, r.snapshotDate, count(p.id))
            from Run r
            left join RunProblem p on p.runId = r.id
            group by r.id, r.status, r.snapshotDate
            order by r.snapshotDate desc, r.id desc
            """)
    List<RunSummary> findAllSummaries();
}
