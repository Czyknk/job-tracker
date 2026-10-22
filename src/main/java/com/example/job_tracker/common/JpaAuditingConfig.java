package com.example.job_tracker.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Osobna klasa konfiguracyjna (a nie adnotacja na klasie main), żeby nie psuć testów slice'owych.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
    
}
