package com.example.job_tracker.auth;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration properties for security settings, including JWT and admin account details.
 */
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(@DefaultValue Jwt jwt, @DefaultValue Admin admin) {

    public record Jwt(
            String secret,
            @DefaultValue("job-tracker") String issuer,
            @DefaultValue("15m") Duration accessTokenTtl) {
    }

    /**
     * Data for the first admin account, created on startup when no users exist in the database.
     */
    public record Admin(String email, String password) {
    }
}
