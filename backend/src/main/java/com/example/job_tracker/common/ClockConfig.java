package com.example.job_tracker.common;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Clock jako bean: serwisy nie wołają Instant.now() wprost, więc w testach można wstrzyknąć stały zegar.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
