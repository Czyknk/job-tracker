package com.example.job_tracker.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import com.example.job_tracker.common.exceptions.BadRequestException;
import com.example.job_tracker.common.exceptions.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-19T12:00:00Z");
    private static final String CURRENT = "current-password-123";
    private static final String NEW = "a-brand-new-password";

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private AccountService service;
    private AppUser user;

    @BeforeEach
    void setUp() {
        service = new AccountService(userRepository, passwordEncoder, refreshTokenRepository,
                Clock.fixed(NOW, ZoneOffset.UTC));
        user = new AppUser("owner@example.com", "old-hash", Role.ADMIN);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
    }

    @Test
    void changingThePasswordStoresTheNewHashAndEndsAllSessions() {
        when(passwordEncoder.matches(CURRENT, "old-hash")).thenReturn(true);
        when(passwordEncoder.encode(NEW)).thenReturn("new-hash");

        service.changePassword(7L, CURRENT, NEW);

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        verify(refreshTokenRepository).revokeAllForUser(7L, NOW);
    }

    @Test
    void aWrongCurrentPasswordIsABadRequestAndChangesNothing() {
        when(passwordEncoder.matches("wrong", "old-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.changePassword(7L, "wrong", NEW)).isInstanceOf(BadRequestException.class);

        assertThat(user.getPasswordHash()).isEqualTo("old-hash");
        verify(refreshTokenRepository, never()).revokeAllForUser(any(), any());
    }

    @Test
    void aWeakNewPasswordIsRejected() {
        when(passwordEncoder.matches(CURRENT, "old-hash")).thenReturn(true);

        assertThatThrownBy(() -> service.changePassword(7L, CURRENT, "short")).isInstanceOf(BadRequestException.class);

        assertThat(user.getPasswordHash()).isEqualTo("old-hash");
    }

    @Test
    void theNewPasswordMustDifferFromTheCurrentOne() {
        when(passwordEncoder.matches(CURRENT, "old-hash")).thenReturn(true);

        assertThatThrownBy(() -> service.changePassword(7L, CURRENT, CURRENT)).isInstanceOf(BadRequestException.class);

        assertThat(user.getPasswordHash()).isEqualTo("old-hash");
    }

    @Test
    void aDisabledOrMissingAccountIsUnauthorized() {
        user.setEnabled(false);

        assertThatThrownBy(() -> service.currentUser(7L)).isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> service.currentUser(99L)).isInstanceOf(UnauthorizedException.class);
    }
}
