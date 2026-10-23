package com.example.job_tracker.jobapplication;

import com.example.job_tracker.common.ResourceNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.example.job_tracker.jobapplication.enums.RecruitmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JobApplicationService {

    private final JobApplicationRepository applicationRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final Clock clock;

    /**
     * Zapisuje aplikację i od razu zakłada pierwszy wpis historii, żeby historia
     * zawsze zaczynała się od bieżącego statusu.
     */
    @Transactional
    public JobApplication create(JobApplication application) {
        JobApplication saved = applicationRepository.save(application);
        statusHistoryRepository.save(
                new StatusHistory(saved, saved.getCurrentStatus(), clock.instant(), null));
        return saved;
    }

    /**
     * Zmienia status: dopisuje wpis do historii i aktualizuje currentStatus w jednej transakcji.
     * changedAt jest opcjonalne (domyślnie "teraz"); currentStatus zawsze odpowiada ostatnio dodanemu wpisowi.
     */
    @Transactional
    public StatusHistory changeStatus(Long applicationId, RecruitmentStatus newStatus,
                                      Instant changedAt, String note) {
        JobApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("JobApplication", applicationId));

        RecruitmentStatus current = application.getCurrentStatus();
        if (current.isTerminal()) {
            throw new InvalidStatusChangeException(current, newStatus);
        }

        Instant effectiveChangedAt = changedAt != null ? changedAt : clock.instant();
        StatusHistory entry = statusHistoryRepository.save(
                new StatusHistory(application, newStatus, effectiveChangedAt, note));

        application.setCurrentStatus(newStatus);
        if (newStatus == RecruitmentStatus.APPLIED && application.getDateApplied() == null) {
            application.setDateApplied(LocalDate.ofInstant(effectiveChangedAt, clock.getZone()));
        }
        return entry;
    }

    @Transactional(readOnly = true)
    public List<StatusHistory> getHistory(Long applicationId) {
        if (!applicationRepository.existsById(applicationId)) {
            throw new ResourceNotFoundException("JobApplication", applicationId);
        }
        return statusHistoryRepository.findByApplicationIdOrderByChangedAtAscIdAsc(applicationId);
    }
}
