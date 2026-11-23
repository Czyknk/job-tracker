package com.example.job_tracker.auth;

import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recovery path for a forgotten password. Start the application once with
 * APP_ADMIN_RESET_PASSWORD=true together with APP_ADMIN_EMAIL (the account to reset) and
 * APP_ADMIN_PASSWORD (the new password), log in, then remove APP_ADMIN_RESET_PASSWORD again.
 * All sessions of the account are ended.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.security.recovery", name = "reset-admin-password", havingValue = "true")
@RequiredArgsConstructor
public class AdminPasswordReset implements ApplicationRunner {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityProperties securityProperties;
    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        SecurityProperties.Admin admin = securityProperties.admin();
        if (isBlank(admin.email()) || isBlank(admin.password())) {
            throw new IllegalStateException(
                    "Password reset requested, but APP_ADMIN_EMAIL and APP_ADMIN_PASSWORD are not both set");
        }
        PasswordPolicy.violation(admin.password()).ifPresent(message -> {
            throw new IllegalStateException("Password reset refused: " + message);
        });

        AppUser user = userRepository.findByEmailIgnoreCase(admin.email().trim())
                .orElseThrow(() -> new IllegalStateException(
                        "Password reset requested, but no user has the e-mail " + admin.email().trim()));

        user.changePasswordHash(passwordEncoder.encode(admin.password()));
        userRepository.save(user);
        refreshTokenRepository.revokeAllForUser(user.getId(), clock.instant());
        log.warn("The password of {} was reset from the configuration and all its sessions were ended. "
                + "Remove APP_ADMIN_RESET_PASSWORD now.", user.getEmail());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
