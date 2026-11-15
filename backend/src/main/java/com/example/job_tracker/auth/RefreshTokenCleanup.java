package com.example.job_tracker.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled cleanup of expired refresh tokens. Runs every day at 3:30 AM.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanup {

    private final RefreshTokenService refreshTokenService;

    @Scheduled(cron = "0 30 3 * * *")
    public void removeExpiredTokens() {
        int removed = refreshTokenService.deleteExpired();
        if (removed > 0) {
            log.info("Removed {} expired refresh tokens", removed);
        }
    }
}
