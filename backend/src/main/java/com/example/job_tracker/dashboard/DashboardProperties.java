package com.example.job_tracker.dashboard;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Thresholds of the dashboard (prefix app.dashboard).
 *
 * @param staleDays    an application waiting this many days since its last status change has had "no response"
 * @param deadlineDays a deadline within this many days from today is "soon"
 * @param limit        maximum number of items per list (each attention reason, upcoming, recent activity)
 */
@Validated
@ConfigurationProperties(prefix = "app.dashboard")
public record DashboardProperties(
        @DefaultValue("14") @Min(1) int staleDays,
        @DefaultValue("7") @Min(0) int deadlineDays,
        @DefaultValue("10") @Min(1) int limit) {
}
