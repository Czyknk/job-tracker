package com.example.job_tracker.jobapplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.job_tracker.common.exceptions.ConflictException;
import com.example.job_tracker.company.Company;
import com.example.job_tracker.company.CompanyRepository;
import com.example.job_tracker.contact.ContactRepository;
import com.example.job_tracker.jobapplication.history.StatusHistory;
import com.example.job_tracker.jobapplication.history.StatusHistoryRepository;
import com.example.job_tracker.jobapplication.model.RecruitmentStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * FOUND is the starting point of every application, so it never outranks a real stage. A stage added
 * with a time earlier than the latest existing stage is a late correction of the timeline and must
 * not change the current status.
 */
@ExtendWith(MockitoExtension.class)
class JobApplicationServiceBackdatingTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-19T12:00:00Z"), ZoneOffset.UTC);
    private static final Instant EARLIER = Instant.parse("2026-11-03T10:00:00Z");
    private static final Instant LATEST = Instant.parse("2026-11-10T10:00:00Z");

    @Mock
    private EntityManager entityManager;

    @Mock
    private JobApplicationRepository applicationRepository;

    @Mock
    private StatusHistoryRepository statusHistoryRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private ContactRepository contactRepository;

    private JobApplicationService service;
    private JobApplication application;

    @BeforeEach
    void setUp() {
        service = new JobApplicationService(
                applicationRepository, statusHistoryRepository, companyRepository, contactRepository, CLOCK, entityManager);
        application = new JobApplication(new Company("Acme"), "Junior Java Developer");
        application.setCurrentStatus(RecruitmentStatus.TECH_INTERVIEW);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        lenient().when(statusHistoryRepository
                .findFirstByApplicationIdAndStatusNotOrderByChangedAtDescIdDesc(1L, RecruitmentStatus.FOUND))
                .thenReturn(Optional.of(
                        new StatusHistory(application, RecruitmentStatus.TECH_INTERVIEW, LATEST, null)));
    }

    @Test
    void aStageOlderThanTheLatestOneDoesNotChangeTheCurrentStatus() {
        when(statusHistoryRepository.save(any(StatusHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        service.changeStatus(1L, RecruitmentStatus.HR_SCREEN, EARLIER, "Forgot to log this call", Set.of());

        assertThat(application.getCurrentStatus()).isEqualTo(RecruitmentStatus.TECH_INTERVIEW);
        verify(statusHistoryRepository).save(any(StatusHistory.class));
    }

    @Test
    void anOlderFinalStatusIsRejected() {
        assertThatThrownBy(() -> service.changeStatus(1L, RecruitmentStatus.REJECTED, EARLIER, null, Set.of()))
                .isInstanceOf(ConflictException.class);

        verify(statusHistoryRepository, never()).save(any());
        assertThat(application.getCurrentStatus()).isEqualTo(RecruitmentStatus.TECH_INTERVIEW);
    }

    @Test
    void aStageWithTheSameTimeAsTheLatestOneBecomesTheCurrentOne() {
        when(statusHistoryRepository.save(any(StatusHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        service.changeStatus(1L, RecruitmentStatus.FINAL_INTERVIEW, LATEST, null, Set.of());

        assertThat(application.getCurrentStatus()).isEqualTo(RecruitmentStatus.FINAL_INTERVIEW);
    }

    @Test
    void theFirstStageCountsEvenWhenItIsOlderThanTheInitialFoundEntry() {
        // A fresh application only has the FOUND entry created "now"; the user records that they applied
        // last week. The stage must win, otherwise the application would stay FOUND (and a backdated
        // REJECTED would be refused).
        application.setCurrentStatus(RecruitmentStatus.FOUND);
        when(statusHistoryRepository
                .findFirstByApplicationIdAndStatusNotOrderByChangedAtDescIdDesc(1L, RecruitmentStatus.FOUND))
                .thenReturn(Optional.empty());
        when(statusHistoryRepository.save(any(StatusHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        service.changeStatus(1L, RecruitmentStatus.APPLIED, EARLIER, null, Set.of());

        assertThat(application.getCurrentStatus()).isEqualTo(RecruitmentStatus.APPLIED);
    }
}
