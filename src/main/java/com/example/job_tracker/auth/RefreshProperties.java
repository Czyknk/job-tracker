package com.example.job_tracker.auth;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration for refresh tokens and session cookie (prefix app.security.refresh).
 */
@ConfigurationProperties(prefix = "app.security.refresh")
public record RefreshProperties(
        @DefaultValue("7d") Duration ttl,
        // true = cookie only over HTTPS; disable only for local HTTP
        @DefaultValue("true") boolean cookieSecure,
        @DefaultValue("refresh_token") String cookieName) {
}
