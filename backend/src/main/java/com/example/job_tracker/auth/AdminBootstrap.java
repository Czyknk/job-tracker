package com.example.job_tracker.auth;

import java.nio.charset.StandardCharsets;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * On the first startup (empty user table), creates an owner account from the configuration.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private static final int MIN_PASSWORD_LENGTH = 12;
    private static final int MAX_PASSWORD_BYTES = 72; // BCrypt truncates passwords longer than 72 bytes.

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityProperties properties;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }

        SecurityProperties.Admin admin = properties.admin();
        if (isBlank(admin.email()) || isBlank(admin.password())) {
            log.warn("No users exist and APP_ADMIN_EMAIL / APP_ADMIN_PASSWORD are not set: nobody will be able to log in");
            return;
        }
        if (admin.password().length() < MIN_PASSWORD_LENGTH
                || admin.password().getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new IllegalStateException("APP_ADMIN_PASSWORD must be at least " + MIN_PASSWORD_LENGTH
                    + " characters and at most " + MAX_PASSWORD_BYTES + " bytes long");
        }

        String email = admin.email().trim().toLowerCase();
        userRepository.save(new AppUser(email, passwordEncoder.encode(admin.password()), Role.ADMIN));
        log.info("Created the initial admin user {}", email);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
