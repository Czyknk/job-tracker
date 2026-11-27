package com.example.job_tracker.jobapplication.dto;

import com.example.job_tracker.jobapplication.JobApplication;
import com.example.job_tracker.jobapplication.model.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Lean representation used in lists: no description and no notes (both can be large).
 * Use the detail endpoint to get the full application.
 */
public record JobApplicationListItem(
        Long id,
        Long companyId,
        String companyName,
        String position,
        String offerUrl,
        JobSource source,
        String location,
        WorkMode workMode,
        EmploymentType employmentType,
        SeniorityLevel seniorityLevel,
        SalaryRangeDto salary,
        LocalDate deadline,
        LocalDate dateFound,
        LocalDate dateApplied,
        RecruitmentStatus currentStatus,
        boolean archived,
        Instant nextStepAt,
        String nextStepNote,
        Long version,
        Instant createdAt,
        Instant updatedAt) {

    // Expects the company to be loaded already (entity graph), because open-in-view is disabled.
    public static JobApplicationListItem from(JobApplication application) {
        return new JobApplicationListItem(
                application.getId(),
                application.getCompany().getId(),
                application.getCompany().getName(),
                application.getPosition(),
                application.getOfferUrl(),
                application.getSource(),
                application.getLocation(),
                application.getWorkMode(),
                application.getEmploymentType(),
                application.getSeniorityLevel(),
                SalaryRangeDto.from(application.getSalary()),
                application.getDeadline(),
                application.getDateFound(),
                application.getDateApplied(),
                application.getCurrentStatus(),
                application.isArchived(),
                application.getNextStepAt(),
                application.getNextStepNote(),
                application.getVersion(),
                application.getCreatedAt(),
                application.getUpdatedAt());
    }
}
