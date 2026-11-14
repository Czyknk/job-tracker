package com.example.job_tracker.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import com.example.job_tracker.common.exceptions.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-14T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final Duration TTL = Duration.ofDays(7);

    @Mock
    private RefreshTokenRepository repository;

    @Mock
    private AppUserRepository userRepository;

    private final UUID familyId = UUID.randomUUID();
    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        RefreshProperties properties = new RefreshProperties(TTL, true, "refresh_token");
        service = new RefreshTokenService(repository, userRepository, properties, CLOCK);
    }

    private RefreshToken storedToken(String raw, Instant expiresAt) {
        RefreshToken token = new RefreshToken(7L, familyId, RefreshTokenService.hash(raw), expiresAt);
        ReflectionTestUtils.setField(token, "id", 100L);
        when(repository.findByTokenHash(RefreshTokenService.hash(raw))).thenReturn(Optional.of(token));
        return token;
    }

    private AppUser user() {
        AppUser user = new AppUser("owner@example.com", "hash", Role.ADMIN);
        ReflectionTestUtils.setField(user, "id", 7L);
        return user;
    }

    @Test
    void startSessionStoresOnlyTheHashOfANewRandomToken() {
        when(repository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshTokenService.IssuedRefreshToken issued = service.startSession(user());

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(saved.capture());
        assertThat(issued.ttl()).isEqualTo(TTL);
        assertThat(saved.getValue().getTokenHash())
                .isNotEqualTo(issued.value())
                .isEqualTo(RefreshTokenService.hash(issued.value()))
                .hasSize(64);
        assertThat(saved.getValue().getExpiresAt()).isEqualTo(NOW.plus(TTL));
        assertThat(saved.getValue().getUserId()).isEqualTo(7L);
    }

    @Test
    void rotateConsumesTheTokenAndIssuesANewOneInTheSameFamily() {
        storedToken("raw-token", NOW.plus(Duration.ofDays(1)));
        AppUser user = user();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(repository.markUsed(100L, NOW)).thenReturn(1);
        when(repository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshTokenService.Rotation rotation = service.rotate("raw-token");

        assertThat(rotation.user()).isSameAs(user);
        assertThat(rotation.refreshToken().value()).isNotEqualTo("raw-token");
        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getFamilyId()).isEqualTo(familyId);
        assertThat(saved.getValue().getTokenHash())
                .isEqualTo(RefreshTokenService.hash(rotation.refreshToken().value()));
    }

    @Test
    void reusingAConsumedTokenRevokesTheWholeFamily() {
        RefreshToken consumed = storedToken("raw-token", NOW.plus(Duration.ofDays(1)));
        ReflectionTestUtils.setField(consumed, "usedAt", NOW.minusSeconds(60));

        assertThatThrownBy(() -> service.rotate("raw-token")).isInstanceOf(UnauthorizedException.class);

        verify(repository).revokeFamily(familyId, NOW);
        verify(repository, never()).save(any());
    }

    @Test
    void anAlreadyRevokedTokenIsRejectedWithoutFurtherChanges() {
        RefreshToken revoked = storedToken("raw-token", NOW.plus(Duration.ofDays(1)));
        ReflectionTestUtils.setField(revoked, "revokedAt", NOW.minusSeconds(60));

        assertThatThrownBy(() -> service.rotate("raw-token")).isInstanceOf(UnauthorizedException.class);

        verify(repository, never()).revokeFamily(any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void anExpiredTokenIsRejected() {
        storedToken("raw-token", NOW);

        assertThatThrownBy(() -> service.rotate("raw-token")).isInstanceOf(UnauthorizedException.class);

        verify(repository, never()).markUsed(any(), any());
    }

    @Test
    void anUnknownTokenIsRejected() {
        when(repository.findByTokenHash(RefreshTokenService.hash("unknown"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rotate("unknown")).isInstanceOf(UnauthorizedException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void aDisabledUserCannotRefresh() {
        storedToken("raw-token", NOW.plus(Duration.ofDays(1)));
        AppUser disabled = user();
        disabled.setEnabled(false);
        when(userRepository.findById(7L)).thenReturn(Optional.of(disabled));

        assertThatThrownBy(() -> service.rotate("raw-token")).isInstanceOf(UnauthorizedException.class);

        verify(repository, never()).markUsed(any(), any());
    }

    @Test
    void theLoserOfAConcurrentRefreshRaceRevokesTheFamily() {
        storedToken("raw-token", NOW.plus(Duration.ofDays(1)));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user()));
        when(repository.markUsed(100L, NOW)).thenReturn(0);

        assertThatThrownBy(() -> service.rotate("raw-token")).isInstanceOf(UnauthorizedException.class);

        verify(repository).revokeFamily(familyId, NOW);
        verify(repository, never()).save(any());
    }

    @Test
    void revokeSessionRevokesTheFamilyOfAKnownToken() {
        storedToken("raw-token", NOW.plus(Duration.ofDays(1)));

        service.revokeSession("raw-token");

        verify(repository).revokeFamily(familyId, NOW);
    }

    @Test
    void revokeSessionIgnoresMissingTokens() {
        service.revokeSession(null);
        service.revokeSession("  ");

        verifyNoInteractions(repository);
    }
}
