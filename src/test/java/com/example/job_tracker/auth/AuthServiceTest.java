package com.example.job_tracker.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    private AuthService service;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode(any())).thenReturn("dummy-hash");
        service = new AuthService(userRepository, passwordEncoder, tokenService);
    }

    @Test
    void returnsTokenForValidCredentials() {
        AppUser user = new AppUser("owner@example.com", "stored-hash", Role.ADMIN);
        TokenService.AccessToken token = new TokenService.AccessToken("jwt", 900);
        when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret-pass", "stored-hash")).thenReturn(true);
        when(tokenService.issueAccessToken(user)).thenReturn(token);

        assertThat(service.login(" owner@example.com ", "secret-pass")).isSameAs(token);
    }

    @Test
    void rejectsWrongPassword() {
        AppUser user = new AppUser("owner@example.com", "stored-hash", Role.ADMIN);
        when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "stored-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login("owner@example.com", "wrong"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid email or password");

        verify(tokenService, never()).issueAccessToken(any());
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
        verify(tokenService, never()).issueAccessToken(any());
    }
}
