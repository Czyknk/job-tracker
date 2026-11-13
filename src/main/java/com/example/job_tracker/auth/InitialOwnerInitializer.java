package com.example.job_tracker.auth;

import lombok.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class InitialOwnerInitializer implements ApplicationRunner {

    private final AppUserRepository users;
    private final AuthProperties properties;
    private final PasswordEncoder passwordEncoder;

    public InitialOwnerInitializer(AppUserRepository users, AuthProperties properties, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.properties = properties;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(@NonNull ApplicationArguments args) {
        if (users.count() == 0) {
            users.save(new AppUser(properties.adminEmail().trim().toLowerCase(),
                    passwordEncoder.encode(properties.adminPassword()), AppUser.Role.OWNER));
        }
    }
}
