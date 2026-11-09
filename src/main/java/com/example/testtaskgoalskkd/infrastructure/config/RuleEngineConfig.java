package com.example.testtaskgoalskkd.infrastructure.config;

import com.example.testtaskgoalskkd.domain.quality.QualityRuleEngine;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RuleEngineConfig {

    @Bean
    public QualityRuleEngine qualityRuleEngine() {
        return new QualityRuleEngine();
    }

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
