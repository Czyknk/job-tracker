package com.example.job_tracker.common;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Clock as a bean: services do not call Instant.now() directly, so in tests you can inject a fixed clock.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(@Value("${app.timezone:UTC}") String timezone) {
        return Clock.system(ZoneId.of(timezone));
    }
}
