package com.example.job_tracker.demo;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings of the public demo mode (prefix app.demo). The demo credentials are intentionally public:
 * the DEMO role is read-only.
 */
@ConfigurationProperties(prefix = "app.demo")
public record DemoProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("demo@job-tracker.example") String email,
        @DefaultValue("demo-readonly-pass") String password) {
}
