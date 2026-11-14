package com.example.job_tracker.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;

import com.example.job_tracker.common.exceptions.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @Mock
    private RefreshTokenService refreshTokenService;

    private AuthService service;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode(any())).thenReturn("dummy-hash");
        service = new AuthService(userRepository, passwordEncoder, tokenService, refreshTokenService);
    }

    @Test
    void loginReturnsAccessAndRefreshTokenForValidCredentials() {
        AppUser user = new AppUser("owner@example.com", "stored-hash", Role.ADMIN);
        TokenService.AccessToken access = new TokenService.AccessToken("jwt", 900);
        RefreshTokenService.IssuedRefreshToken refresh =
                new RefreshTokenService.IssuedRefreshToken("raw", Duration.ofDays(7));
        when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret-pass", "stored-hash")).thenReturn(true);
        when(tokenService.issueAccessToken(user)).thenReturn(access);
        when(refreshTokenService.startSession(user)).thenReturn(refresh);

        AuthService.Session session = service.login(" owner@example.com ", "secret-pass");

        assertThat(session.accessToken()).isSameAs(access);
        assertThat(session.refreshToken()).isSameAs(refresh);
    }

    @Test
    void loginRejectsWrongPasswordWithoutStartingASession() {
        AppUser user = new AppUser("owner@example.com", "stored-hash", Role.ADMIN);
        when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "stored-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login("owner@example.com", "wrong"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid email or password");

        verify(tokenService, never()).issueAccessToken(any());
        verifyNoInteractions(refreshTokenService);
    }

    @Test
    void unknownUserIsRejectedAfterAComparableHashCheck() {
        when(userRepository.findByEmailIgnoreCase("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login("nobody@example.com", "whatever"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid email or password");

        // timing attack prevention: the password is checked against a dummy hash
        verify(passwordEncoder).matches("whatever", "dummy-hash");
    }

    @Test
    void disabledUserIsRejectedEvenWithCorrectPassword() {
        AppUser user = new AppUser("owner@example.com", "stored-hash", Role.ADMIN);
        user.setEnabled(false);
        when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.login("owner@example.com", "secret-pass"))
                .isInstanceOf(UnauthorizedException.class);

        verify(passwordEncoder, never()).matches("secret-pass", "stored-hash");
        verifyNoInteractions(refreshTokenService);
    }

    @Test
    void refreshIssuesNewAccessTokenFromRotation() {
        AppUser user = new AppUser("owner@example.com", "stored-hash", Role.ADMIN);
        RefreshTokenService.IssuedRefreshToken next =
                new RefreshTokenService.IssuedRefreshToken("next", Duration.ofDays(7));
        TokenService.AccessToken access = new TokenService.AccessToken("jwt2", 900);
        when(refreshTokenService.rotate("old")).thenReturn(new RefreshTokenService.Rotation(user, next));
        when(tokenService.issueAccessToken(user)).thenReturn(access);

        AuthService.Session session = service.refresh("old");

        assertThat(session.accessToken()).isSameAs(access);
        assertThat(session.refreshToken()).isSameAs(next);
    }

    @Test
    void refreshWithBlankTokenIsRejectedWithoutTouchingTheStore() {
        assertThatThrownBy(() -> service.refresh("  ")).isInstanceOf(UnauthorizedException.class);

        verifyNoInteractions(refreshTokenService);
    }

    @Test
    void logoutRevokesTheSession() {
        service.logout("raw-token");

        verify(refreshTokenService).revokeSession("raw-token");
    }
}
