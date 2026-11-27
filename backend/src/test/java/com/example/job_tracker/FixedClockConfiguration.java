package com.example.job_tracker;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * A fixed clock for integration tests of date-based rules. It is late evening in UTC but already the next day in
 * Warsaw, so tests also check that "today" follows the application time zone and not UTC.
 */
@TestConfiguration(proxyBeanMethods = false)
public class FixedClockConfiguration {

    public static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");
    public static final Instant NOW = Instant.parse("2026-11-27T23:30:00Z");
    // NOW in Warsaw is 2026-11-28 00:30
    public static final LocalDate TODAY = LocalDate.of(2026, 11, 28);

    @Bean
    @Primary
    Clock fixedClock() {
        return Clock.fixed(NOW, ZONE);
    }
}
