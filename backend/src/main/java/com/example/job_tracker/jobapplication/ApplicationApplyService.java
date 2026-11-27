package com.example.job_tracker.jobapplication;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import com.example.job_tracker.common.exceptions.BadRequestException;
import com.example.job_tracker.common.exceptions.ConflictException;
import com.example.job_tracker.jobapplication.documents.ApplicationDocumentService;
import com.example.job_tracker.jobapplication.dto.ApplyRequest;
import com.example.job_tracker.jobapplication.model.RecruitmentStatus;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Mark as applied" in one transaction: attaches the documents that were sent and adds the APPLIED history entry
 * (which also sets dateApplied). It only orchestrates the existing services, so the usual history and document
 * rules apply unchanged.
 */
@Service
@RequiredArgsConstructor
public class ApplicationApplyService {

    private final JobApplicationService applicationService;
    private final ApplicationDocumentService documentService;
    private final EntityManager entityManager;
    private final Clock clock;

    @Transactional
    public JobApplication apply(Long applicationId, ApplyRequest request) {
        ApplyRequest body = request != null ? request : new ApplyRequest(null, null, null);
        JobApplication application = applicationService.get(applicationId);

        // Only FOUND -> APPLIED: applying later would put APPLIED after real stages and move the status backwards
        if (application.getDateApplied() != null) {
            throw new ConflictException("The application is already applied (dateApplied "
                    + application.getDateApplied() + ")");
        }
        if (application.getCurrentStatus() != RecruitmentStatus.FOUND) {
            throw new ConflictException("Only an application in FOUND can be marked as applied; current status is "
                    + application.getCurrentStatus());
        }

        Instant appliedAt = body.appliedAt() != null ? body.appliedAt() : clock.instant();
        if (LocalDate.ofInstant(appliedAt, clock.getZone()).isBefore(application.getDateFound())) {
            throw new BadRequestException("dateApplied cannot be before dateFound");
        }

        // Documents first: a missing or archived document stops the operation before the history changes
        Set<Long> documentIds = body.documentIds() != null ? body.documentIds() : Set.of();
        documentIds.forEach(documentId -> documentService.attach(applicationId, documentId));

        applicationService.changeStatus(applicationId, RecruitmentStatus.APPLIED, appliedAt, body.note());

        // Write the changes and re-read the derived values (version, lastStatusChangedAt) for the response
        entityManager.flush();
        entityManager.refresh(application);
        return application;
    }
}
