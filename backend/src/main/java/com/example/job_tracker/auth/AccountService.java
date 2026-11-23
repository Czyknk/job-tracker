package com.example.job_tracker.auth;

import java.time.Clock;

import com.example.job_tracker.common.exceptions.BadRequestException;
import com.example.job_tracker.common.exceptions.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Operations on the account of the logged-in user.
 */
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public AppUser currentUser(Long userId) {
        return userRepository.findById(userId)
                .filter(AppUser::isEnabled)
                .orElseThrow(() -> new UnauthorizedException("The account no longer exists or is disabled"));
    }

    /**
     * Changes the password and ends every session (all refresh tokens are revoked). Access tokens that
     * were already issued stay valid until they expire (at most their short TTL). The BCrypt work happens
     * inside the transaction: this endpoint is rarely used, so holding a connection briefly is fine.
     * Validation failures are 400 (not 401), so that a client does not mistake them for an expired session.
     */
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        AppUser user = currentUser(userId);

        if (!matches(currentPassword, user.getPasswordHash())) {
            throw new BadRequestException("The current password is incorrect");
        }
        PasswordPolicy.violation(newPassword).ifPresent(message -> {
            throw new BadRequestException(message);
        });
        if (currentPassword.equals(newPassword)) {
            throw new BadRequestException("The new password must differ from the current one");
        }

        user.changePasswordHash(passwordEncoder.encode(newPassword));
        refreshTokenRepository.revokeAllForUser(userId, clock.instant());
    }

    private boolean matches(String rawPassword, String hash) {
        try {
            return passwordEncoder.matches(rawPassword, hash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
