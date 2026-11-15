package com.example.job_tracker.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import com.example.job_tracker.common.exceptions.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sessions based on refresh tokens with rotation and reuse detection.
 * Each token is one-time use: using it issues a new token in the same "family". If someone uses
 * an already used token (e.g., a stolen copy), the whole family is revoked.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final AppUserRepository userRepository;
    private final RefreshProperties properties;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    /**
     * Issued refresh token with its TTL. The raw value is returned only once, when the token is created.
     */
    public record IssuedRefreshToken(String value, Duration ttl) {
    }

    public record Rotation(AppUser user, IssuedRefreshToken refreshToken) {
    }

    /**
     * Starts a new session (a new family of tokens), e.g., after logging in.
     */
    @Transactional
    public IssuedRefreshToken startSession(AppUser user) {
        return create(user.getId(), UUID.randomUUID(), clock.instant());
    }

    /**
     * Rotates the refresh token: marks the current token as used and issues a new one in the same family.
     * If the current token was already used or revoked, the whole family is revoked and an exception is thrown.
     */
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public Rotation rotate(String rawToken) {
        Instant now = clock.instant();
        RefreshToken current = repository.findByTokenHash(hash(rawToken))
                .orElseThrow(RefreshTokenService::invalid);

        if (current.getRevokedAt() != null) {
            throw invalid(); // token was revoked, e.g., due to reuse of another token in the same family
        }
        if (current.getUsedAt() != null) {
            repository.revokeFamily(current.getFamilyId(), now);
            log.warn("Refresh token reuse detected for user {}: the whole session was revoked",
                    current.getUserId());
            throw invalid();
        }
        if (!current.getExpiresAt().isAfter(now)) {
            throw invalid();
        }

        AppUser user = userRepository.findById(current.getUserId())
                .filter(AppUser::isEnabled)
                .orElseThrow(RefreshTokenService::invalid);

        if (repository.markUsed(current.getId(), now) == 0) {
            // Another request used the same token concurrently, so it was marked as used first. Revoke the whole family.
            repository.revokeFamily(current.getFamilyId(), now);
            throw invalid();
        }
        return new Rotation(user, create(user.getId(), current.getFamilyId(), now));
    }

    /**
     * Revokes the entire session. Idempotent; unknown or empty tokens are ignored.
     */
    @Transactional
    public void revokeSession(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        Instant now = clock.instant();
        repository.findByTokenHash(hash(rawToken))
                .ifPresent(token -> repository.revokeFamily(token.getFamilyId(), now));
    }

    @Transactional
    public int deleteExpired() {
        return repository.deleteExpiredBefore(clock.instant());
    }

    private IssuedRefreshToken create(Long userId, UUID familyId, Instant now) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        Duration ttl = properties.ttl();
        repository.save(new RefreshToken(userId, familyId, hash(raw), now.plus(ttl)));
        return new IssuedRefreshToken(raw, ttl);
    }

    private static UnauthorizedException invalid() {
        return new UnauthorizedException("Invalid refresh token");
    }
    
    // Hashes the token using SHA-256 and encodes it as a hex string. The raw token is never stored in the database.
    static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
