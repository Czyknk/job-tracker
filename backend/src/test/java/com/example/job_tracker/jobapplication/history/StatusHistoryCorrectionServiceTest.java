package com.example.job_tracker.jobapplication.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import com.example.job_tracker.common.exceptions.ConflictException;
import com.example.job_tracker.common.exceptions.ResourceNotFoundException;
import com.example.job_tracker.company.Company;
import com.example.job_tracker.jobapplication.JobApplication;
import com.example.job_tracker.jobapplication.JobApplicationRepository;
import com.example.job_tracker.jobapplication.model.RecruitmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StatusHistoryCorrectionServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-19T12:00:00Z"), ZoneOffset.UTC);
    private static final Instant T0 = Instant.parse("2026-11-01T10:00:00Z");
    private static final Instant T1 = Instant.parse("2026-11-05T10:00:00Z");
    private static final Instant T2 = Instant.parse("2026-11-10T10:00:00Z");
    private static final Instant T3 = Instant.parse("2026-11-15T10:00:00Z");

    @Mock
    private JobApplicationRepository applicationRepository;

    @Mock
    private StatusHistoryRepository historyRepository;

    private StatusHistoryCorrectionService service;
    private JobApplication application;

    @BeforeEach
    void setUp() {
        service = new StatusHistoryCorrectionService(applicationRepository, historyRepository, CLOCK);
        application = new JobApplication(new Company("Acme"), "Junior Java Developer");
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
    }

    private StatusHistory entry(long id, RecruitmentStatus status, Instant at) {
        StatusHistory entry = new StatusHistory(application, status, at, null);
        ReflectionTestUtils.setField(entry, "id", id);
        return entry;
    }

    private void timeline(StatusHistory... entries) {
        when(historyRepository.findByApplicationIdOrderByChangedAtAscIdAsc(1L)).thenReturn(List.of(entries));
    }

    @Test
    void updateRecomputesTheCurrentStatusFromTheLatestEntry() {
        StatusHistory applied = entry(11, RecruitmentStatus.APPLIED, T1);
        timeline(entry(10, RecruitmentStatus.FOUND, T0), applied, entry(12, RecruitmentStatus.HR_SCREEN, T2));
        application.setCurrentStatus(RecruitmentStatus.HR_SCREEN);

        // the APPLIED entry is moved after the HR screen, so it becomes the latest one
        service.update(1L, 11L, RecruitmentStatus.APPLIED, T3, "Corrected time");

        assertThat(application.getCurrentStatus()).isEqualTo(RecruitmentStatus.APPLIED);
        assertThat(applied.getChangedAt()).isEqualTo(T3);
        assertThat(applied.getNote()).isEqualTo("Corrected time");
    }

    @Test
    void updateRejectsAFinalStatusThatWouldNotBeLastAndChangesNothing() {
        StatusHistory found = entry(10, RecruitmentStatus.FOUND, T0);
        timeline(found, entry(11, RecruitmentStatus.APPLIED, T1));

        assertThatThrownBy(() -> service.update(1L, 10L, RecruitmentStatus.REJECTED, T0, null))
                .isInstanceOf(ConflictException.class);

        assertThat(found.getStatus()).isEqualTo(RecruitmentStatus.FOUND);
        assertThat(application.getCurrentStatus()).isEqualTo(RecruitmentStatus.FOUND);
    }

    @Test
    void deletingTheLastEntryRestoresThePreviousStatus() {
        StatusHistory rejected = entry(12, RecruitmentStatus.REJECTED, T2);
        timeline(entry(10, RecruitmentStatus.FOUND, T0), entry(11, RecruitmentStatus.APPLIED, T1), rejected);
        application.setCurrentStatus(RecruitmentStatus.REJECTED);

        service.delete(1L, 12L);

        assertThat(application.getCurrentStatus()).isEqualTo(RecruitmentStatus.APPLIED);
        verify(historyRepository).delete(rejected);
    }

    @Test
    void theOnlyEntryCannotBeDeleted() {
        timeline(entry(10, RecruitmentStatus.FOUND, T0));

        assertThatThrownBy(() -> service.delete(1L, 10L)).isInstanceOf(ConflictException.class);

        verify(historyRepository, never()).delete(any());
    }

    @Test
    void deletingTheAppliedEntryClearsADateAppliedDerivedFromIt() {
        StatusHistory applied = entry(11, RecruitmentStatus.APPLIED, T1);
        timeline(entry(10, RecruitmentStatus.FOUND, T0), applied);
        application.setCurrentStatus(RecruitmentStatus.APPLIED);
        application.setDateApplied(LocalDate.of(2026, 11, 5));

        service.delete(1L, 11L);

        assertThat(application.getDateApplied()).isNull();
        assertThat(application.getCurrentStatus()).isEqualTo(RecruitmentStatus.FOUND);
    }

    @Test
    void deletingTheAppliedEntryKeepsADateAppliedTypedInByHand() {
        timeline(entry(10, RecruitmentStatus.FOUND, T0), entry(11, RecruitmentStatus.APPLIED, T1));
        application.setDateApplied(LocalDate.of(2026, 10, 30));

        service.delete(1L, 11L);

        assertThat(application.getDateApplied()).isEqualTo(LocalDate.of(2026, 10, 30));
    }

    @Test
    void unknownEntryIsNotFound() {
        timeline(entry(10, RecruitmentStatus.FOUND, T0));

        assertThatThrownBy(() -> service.delete(1L, 99L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.update(1L, 99L, RecruitmentStatus.APPLIED, T1, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
