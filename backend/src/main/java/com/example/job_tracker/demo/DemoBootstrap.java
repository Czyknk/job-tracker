package com.example.job_tracker.demo;

import com.example.job_tracker.auth.AppUser;
import com.example.job_tracker.auth.AppUserRepository;
import com.example.job_tracker.auth.Role;
import com.example.job_tracker.auth.SecurityProperties;
import com.example.job_tracker.company.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Prepares a dedicated demo instance: creates the read-only DEMO user and fills an empty database with
 * sample data. It refuses to run next to a real owner account, because the application has no
 * per-user data isolation and the DEMO user would see the owner's private data.
 * Runs before AdminBootstrap, so the admin bootstrap sees an existing user and does nothing.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(prefix = "app.demo", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class DemoBootstrap implements ApplicationRunner {

    private static final int MIN_PASSWORD_LENGTH = 12;
    private static final int MAX_PASSWORD_BYTES = 72; // BCrypt limit

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DemoProperties demoProperties;
    private final SecurityProperties securityProperties;
    private final CompanyRepository companyRepository;
    private final DemoDataSeeder seeder;

    @Override
    public void run(ApplicationArguments args) {
        String adminEmail = securityProperties.admin().email();
        if (adminEmail != null && !adminEmail.isBlank()) {
            throw new IllegalStateException(
                    "Demo mode cannot be combined with an admin account: unset APP_ADMIN_EMAIL and APP_ADMIN_PASSWORD");
        }

        String password = demoProperties.password();
        if (password == null || password.length() < MIN_PASSWORD_LENGTH
                || password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new IllegalStateException("The demo password must be at least " + MIN_PASSWORD_LENGTH
                    + " characters and at most " + MAX_PASSWORD_BYTES + " bytes long");
        }

        if (userRepository.existsByRole(Role.ADMIN)) {
            throw new IllegalStateException("Refusing to enable demo mode: the database already contains an "
                    + "ADMIN user. Demo mode is meant for dedicated demo instances only");
        }

        String email = demoProperties.email().trim().toLowerCase();
        if (userRepository.findByEmailIgnoreCase(email).isEmpty()) {
            userRepository.save(new AppUser(email, passwordEncoder.encode(password), Role.DEMO));
            log.info("Created the read-only demo user {}", email);
        }

        if (companyRepository.count() == 0) {
            seeder.seed();
            log.info("Seeded the demo database with sample data");
        }
    }
}
