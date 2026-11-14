package com.example.job_tracker.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final SecurityProperties properties;
    private final Clock clock;

    public AccessToken issueAccessToken(AppUser user) {
        Instant now = clock.instant();
        Duration ttl = properties.jwt().accessTokenTtl();

        // Build the JWT claims set with the necessary information
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.jwt().issuer())
                .subject(String.valueOf(user.getId()))
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .id(UUID.randomUUID().toString())
                .claim("role", user.getRole().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AccessToken(token, ttl.toSeconds());
    }

    public record AccessToken(String value, long expiresInSeconds) {
    }
}
