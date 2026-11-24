package com.example.job_tracker.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminPasswordResetTest {

    private static final Instant NOW = Instant.parse("2026-11-19T12:00:00Z");

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private AdminPasswordReset reset(String email, String password) {
        SecurityProperties properties = new SecurityProperties(
                new SecurityProperties.Jwt("irrelevant", "job-tracker", Duration.ofMinutes(15)),
                new SecurityProperties.Admin(email, password));
        return new AdminPasswordReset(userRepository, passwordEncoder, properties, refreshTokenRepository,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void replacesThePasswordAndEndsTheSessionsOfTheConfiguredAccount() {
        AppUser user = new AppUser("owner@example.com", "old-hash", Role.ADMIN);
        ReflectionTestUtils.setField(user, "id", 7L);
        when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("a-brand-new-password")).thenReturn("new-hash");

        reset(" owner@example.com ", "a-brand-new-password").run(null);

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        verify(userRepository).save(user);
        verify(refreshTokenRepository).revokeAllForUser(7L, NOW);
    }

    @Test
    void refusesWhenTheCredentialsAreNotConfigured() {
        assertThatThrownBy(() -> reset(null, null).run(null)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void refusesAWeakPassword() {
        assertThatThrownBy(() -> reset("owner@example.com", "short").run(null))
                .isInstanceOf(IllegalStateException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void refusesWhenNoUserHasTheConfiguredEmail() {
        when(userRepository.findByEmailIgnoreCase("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reset("nobody@example.com", "a-brand-new-password").run(null))
                .isInstanceOf(IllegalStateException.class);
    }
}
