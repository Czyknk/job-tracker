package com.example.job_tracker.auth;

import java.time.Instant;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Service
public class TokenService {

    private final AuthProperties properties;
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;

    public TokenService(AuthProperties properties) {
        this.properties = properties;
        SecretKey key = new SecretKeySpec(properties.jwtSecret().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        this.decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    public String issue(AppUser user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("job-tracker")
                .issuedAt(now)
                .expiresAt(now.plus(properties.tokenTtl()))
                .subject(user.getUsername())
                .claim("role", user.getRole().name())
                .build();
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    @Bean
    JwtDecoder jwtDecoder() {
        return decoder;
    }
}
