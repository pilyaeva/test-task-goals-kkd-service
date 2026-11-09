package com.example.testtaskgoalskkd.infrastructure.repository;

import com.example.testtaskgoalskkd.domain.KeyResult;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KeyResultRepository extends JpaRepository<KeyResult, String> {

    List<KeyResult> findByGoalIdIn(Collection<String> goalIds);
}
