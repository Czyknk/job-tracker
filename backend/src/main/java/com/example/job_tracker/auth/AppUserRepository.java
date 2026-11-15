package com.example.job_tracker.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    boolean existsByRole(Role role);
    Optional<AppUser> findByEmailIgnoreCase(String email);
}
