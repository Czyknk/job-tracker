package com.example.job_tracker.jobapplication.dto;

import com.example.job_tracker.jobapplication.JobApplication;
import com.example.job_tracker.jobapplication.model.*;

import java.time.Instant;
import java.time.LocalDate;

public record JobApplicationResponse(
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
        String rawDescription,
        LocalDate deadline,
        LocalDate dateFound,
        LocalDate dateApplied,
        RecruitmentStatus currentStatus,
        String notes,
        boolean archived,
        Instant nextStepAt,
        String nextStepNote,
        Long version,
        Instant createdAt,
        Instant updatedAt) {

    // Assumes that company is already loaded (EntityGraph in repository), because open-in-view is disabled.
    public static JobApplicationResponse from(JobApplication application) {
        return new JobApplicationResponse(
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
                application.getRawDescription(),
                application.getDeadline(),
                application.getDateFound(),
                application.getDateApplied(),
                application.getCurrentStatus(),
                application.getNotes(),
                application.isArchived(),
                application.getNextStepAt(),
                application.getNextStepNote(),
                application.getVersion(),
                application.getCreatedAt(),
                application.getUpdatedAt());
    }
}
