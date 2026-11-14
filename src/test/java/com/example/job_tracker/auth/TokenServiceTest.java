package com.example.job_tracker.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class TokenServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    private final SecretKey key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    private final JwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
            .macAlgorithm(MacAlgorithm.HS256).build();
    private final SecurityProperties properties = new SecurityProperties(
            new SecurityProperties.Jwt(SECRET, "job-tracker", Duration.ofMinutes(15)),
            new SecurityProperties.Admin(null, null));

    private AppUser user;

    @BeforeEach
    void setUp() {
        user = new AppUser("owner@example.com", "hash", Role.ADMIN);
        ReflectionTestUtils.setField(user, "id", 7L);
    }

    private TokenService serviceWith(Clock clock) {
        return new TokenService(new NimbusJwtEncoder(new ImmutableSecret<>(key)), properties, clock);
    }

    @Test
    void issuesDecodableTokenWithExpectedClaims() {
        TokenService.AccessToken token = serviceWith(Clock.systemUTC()).issueAccessToken(user);

        Jwt jwt = decoder.decode(token.value());

        assertThat(jwt.getSubject()).isEqualTo("7");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("ADMIN");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("job-tracker");
        assertThat(jwt.getId()).isNotBlank();
        assert jwt.getExpiresAt() != null;
        assert jwt.getIssuedAt() != null;
        assertThat(jwt.getExpiresAt().getEpochSecond() - jwt.getIssuedAt().getEpochSecond()).isEqualTo(900);
        assertThat(token.expiresInSeconds()).isEqualTo(900);
    }

    @Test
    void tokenIssuedLongAgoIsRejectedAsExpired() {
        Clock past = Clock.fixed(Instant.parse("2020-01-01T00:00:00Z"), ZoneOffset.UTC);
        String token = serviceWith(past).issueAccessToken(user).value();

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        SecretKey otherKey = new SecretKeySpec(
                "another-secret-another-secret-123".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        TokenService otherService = new TokenService(
                new NimbusJwtEncoder(new ImmutableSecret<>(otherKey)), properties, Clock.systemUTC());
        String token = otherService.issueAccessToken(user).value();

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }
}
