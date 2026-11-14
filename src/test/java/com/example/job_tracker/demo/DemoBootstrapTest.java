package com.example.job_tracker.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;

import com.example.job_tracker.auth.AppUser;
import com.example.job_tracker.auth.AppUserRepository;
import com.example.job_tracker.auth.Role;
import com.example.job_tracker.auth.SecurityProperties;
import com.example.job_tracker.company.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class DemoBootstrapTest {

    private static final String DEMO_EMAIL = "demo@job-tracker.example";
    private static final String DEMO_PASSWORD = "demo-readonly-pass";

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private DemoDataSeeder seeder;

    private DemoBootstrap bootstrap(String adminEmail, String demoPassword) {
        SecurityProperties security = new SecurityProperties(
                new SecurityProperties.Jwt("irrelevant", "job-tracker", Duration.ofMinutes(15)),
                new SecurityProperties.Admin(adminEmail, null));
        DemoProperties demo = new DemoProperties(true, DEMO_EMAIL, demoPassword);
        return new DemoBootstrap(userRepository, passwordEncoder, demo, security, companyRepository, seeder);
    }

    @Test
    void refusesToRunWhenAnAdminAccountIsConfigured() {
        assertThatThrownBy(() -> bootstrap("owner@example.com", DEMO_PASSWORD).run(null))
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(userRepository, seeder);
    }

    @Test
    void refusesToRunWhenTheDemoPasswordIsWeak() {
        assertThatThrownBy(() -> bootstrap(null, "short").run(null))
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(userRepository, seeder);
    }

    @Test
    void refusesToRunWhenAnAdminUserAlreadyExists() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(true);

        assertThatThrownBy(() -> bootstrap(null, DEMO_PASSWORD).run(null))
                .isInstanceOf(IllegalStateException.class);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(seeder);
    }

    @Test
    void createsTheDemoUserAndSeedsAnEmptyInstance() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
        when(userRepository.findByEmailIgnoreCase(DEMO_EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(DEMO_PASSWORD)).thenReturn("hashed");
        when(companyRepository.count()).thenReturn(0L);

        bootstrap(null, DEMO_PASSWORD).run(null);

        ArgumentCaptor<AppUser> saved = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo(DEMO_EMAIL);
        assertThat(saved.getValue().getRole()).isEqualTo(Role.DEMO);
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("hashed");
        verify(seeder).seed();
    }

    @Test
    void doesNotRepeatWorkOnAnAlreadyPreparedInstance() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
        when(userRepository.findByEmailIgnoreCase(DEMO_EMAIL))
                .thenReturn(Optional.of(new AppUser(DEMO_EMAIL, "hash", Role.DEMO)));
        when(companyRepository.count()).thenReturn(5L);

        bootstrap(null, DEMO_PASSWORD).run(null);

        verify(userRepository, never()).save(any());
        verify(seeder, never()).seed();
    }
}
