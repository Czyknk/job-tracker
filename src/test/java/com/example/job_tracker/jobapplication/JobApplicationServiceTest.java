package com.example.job_tracker.jobapplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import com.example.job_tracker.common.ResourceNotFoundException;
import com.example.job_tracker.company.Company;
import com.example.job_tracker.jobapplication.enums.RecruitmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobApplicationServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-10-22T10:00:00Z"), ZoneOffset.UTC);

    @Mock
    private JobApplicationRepository applicationRepository;

    @Mock
    private StatusHistoryRepository statusHistoryRepository;

    private JobApplicationService service;
    private JobApplication application;

    @BeforeEach
    void setUp() {
        service = new JobApplicationService(applicationRepository, statusHistoryRepository, FIXED_CLOCK);
        application = new JobApplication(new Company("Acme"), "Junior Java Developer");
    }

    @Test
    void changeStatusSavesHistoryEntryAndUpdatesCurrentStatus() {
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(statusHistoryRepository.save(any(StatusHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        StatusHistory entry = service.changeStatus(1L, RecruitmentStatus.HR_SCREEN, null, "Call scheduled");

        assertThat(application.getCurrentStatus()).isEqualTo(RecruitmentStatus.HR_SCREEN);
        assertThat(entry.getStatus()).isEqualTo(RecruitmentStatus.HR_SCREEN);
        assertThat(entry.getChangedAt()).isEqualTo(FIXED_CLOCK.instant());
        assertThat(entry.getNote()).isEqualTo("Call scheduled");
        assertThat(entry.getApplication()).isSameAs(application);
    }

    @Test
    void movingToAppliedSetsDateAppliedFromChangedAt() {
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(statusHistoryRepository.save(any(StatusHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        service.changeStatus(1L, RecruitmentStatus.APPLIED, Instant.parse("2026-10-20T08:30:00Z"), null);

        assertThat(application.getDateApplied()).isEqualTo(LocalDate.of(2026, 10, 20));
    }

    @Test
    void movingToAppliedDoesNotOverwriteExistingDateApplied() {
        application.setDateApplied(LocalDate.of(2026, 10, 1));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(statusHistoryRepository.save(any(StatusHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        service.changeStatus(1L, RecruitmentStatus.APPLIED, null, null);

        assertThat(application.getDateApplied()).isEqualTo(LocalDate.of(2026, 10, 1));
    }

    @Test
    void rejectsStatusChangeFromFinalStatus() {
        application.setCurrentStatus(RecruitmentStatus.REJECTED);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> service.changeStatus(1L, RecruitmentStatus.TECH_INTERVIEW, null, null))
                .isInstanceOf(InvalidStatusChangeException.class);

        verifyNoInteractions(statusHistoryRepository);
        assertThat(application.getCurrentStatus()).isEqualTo(RecruitmentStatus.REJECTED);
    }

    @Test
    void throwsWhenApplicationDoesNotExist() {
        when(applicationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeStatus(99L, RecruitmentStatus.APPLIED, null, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createWritesInitialHistoryEntry() {
        when(applicationRepository.save(application)).thenReturn(application);

        service.create(application);

        ArgumentCaptor<StatusHistory> captor = ArgumentCaptor.forClass(StatusHistory.class);
        verify(statusHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(RecruitmentStatus.FOUND);
        assertThat(captor.getValue().getChangedAt()).isEqualTo(FIXED_CLOCK.instant());
        assertThat(captor.getValue().getApplication()).isSameAs(application);
    }
}
