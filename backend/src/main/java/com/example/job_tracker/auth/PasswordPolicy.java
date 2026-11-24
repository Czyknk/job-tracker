package com.example.job_tracker.auth;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Password rules shared by the account endpoints and the recovery runner.
 */
final class PasswordPolicy {

    static final int MIN_LENGTH = 12;
    static final int MAX_BYTES = 72; // BCrypt limit

    private PasswordPolicy() {
    }

    static Optional<String> violation(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            return Optional.of("The password must be at least " + MIN_LENGTH + " characters long");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            return Optional.of("The password must be at most " + MAX_BYTES + " bytes long");
        }
        return Optional.empty();
    }
}
