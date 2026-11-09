package com.example.testtaskgoalskkd.infrastructure.repository;

import com.example.testtaskgoalskkd.domain.Rule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuleRepository extends JpaRepository<Rule, String> {

    boolean existsByCode(String code);
}
