package com.example.job_tracker.auth;

import java.util.Optional;

import com.example.job_tracker.common.exceptions.UnauthorizedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Login with email + password. Intentionally without @Transactional: BCrypt hashing takes time and should not
 * hold a database connection (user retrieval has its own short transaction).
 */
@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid email or password";
    private static final String INVALID_REFRESH_TOKEN = "Invalid refresh token";

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;

    // Dummy hash for timing attack prevention: always call passwordEncoder.matches() even if the user doesn't exist.
    private final String dummyHash;

    public AuthService(AppUserRepository userRepository, PasswordEncoder passwordEncoder,
                       TokenService tokenService, RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.refreshTokenService = refreshTokenService;
        this.dummyHash = passwordEncoder.encode("not-a-real-password");
    }

    /**
     * Result of authentication: access token (for the response) and refresh token (for the cookie).
     */
    public record Session(TokenService.AccessToken accessToken,
                          RefreshTokenService.IssuedRefreshToken refreshToken) {
    }

    public Session login(String email, String password) {
        Optional<AppUser> user = userRepository.findByEmailIgnoreCase(email.trim());

        boolean authenticated;
        if (user.isPresent() && user.get().isEnabled()) {
            authenticated = matches(password, user.get().getPasswordHash());
        } else {
            matches(password, dummyHash);
            authenticated = false;
        }

        if (!authenticated) {
            // Do not reveal whether the email exists or not, to prevent user enumeration attacks.
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }
        AppUser authenticatedUser = user.get();
        return new Session(
                tokenService.issueAccessToken(authenticatedUser),
                refreshTokenService.startSession(authenticatedUser));
    }

    public Session refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new UnauthorizedException(INVALID_REFRESH_TOKEN);
        }
        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(rawRefreshToken);
        return new Session(tokenService.issueAccessToken(rotation.user()), rotation.refreshToken());
    }

    public void logout(String rawRefreshToken) {
        refreshTokenService.revokeSession(rawRefreshToken);
    }

    private boolean matches(String rawPassword, String hash) {
        try {
            return passwordEncoder.matches(rawPassword, hash);
        } catch (IllegalArgumentException e) {
            // BCryptPasswordEncoder throws IllegalArgumentException if the hash is invalid (e.g., not a valid BCrypt hash).
            return false;
        }
    }
}
