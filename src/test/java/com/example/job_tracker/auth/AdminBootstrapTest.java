package com.example.job_tracker.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapTest {

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminBootstrap bootstrapWith(String email, String password) {
        SecurityProperties properties = new SecurityProperties(
                new SecurityProperties.Jwt("irrelevant", "job-tracker", Duration.ofMinutes(15)),
                new SecurityProperties.Admin(email, password));
        return new AdminBootstrap(userRepository, passwordEncoder, properties);
    }

    @Test
    void createsAdminWithHashedPasswordWhenNoUsersExist() {
        when(userRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode("a-long-enough-password")).thenReturn("hashed");

        bootstrapWith("  Owner@Example.com ", "a-long-enough-password").run(null);

        ArgumentCaptor<AppUser> saved = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("owner@example.com");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("hashed");
        assertThat(saved.getValue().getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void doesNothingWhenUsersAlreadyExist() {
        when(userRepository.count()).thenReturn(1L);

        bootstrapWith("owner@example.com", "a-long-enough-password").run(null);

        verify(userRepository, never()).save(any());
    }

    @Test
    void doesNothingWhenAdminIsNotConfigured() {
        when(userRepository.count()).thenReturn(0L);

        bootstrapWith(null, null).run(null);

        verify(userRepository, never()).save(any());
    }

    @Test
    void refusesToStartWithAWeakAdminPassword() {
        when(userRepository.count()).thenReturn(0L);

        assertThatThrownBy(() -> bootstrapWith("owner@example.com", "short").run(null))
                .isInstanceOf(IllegalStateException.class);

        verify(userRepository, never()).save(any());
    }
}
