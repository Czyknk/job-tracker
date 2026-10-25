package com.example.job_tracker.jobapplication.dto;

import com.example.job_tracker.jobapplication.JobApplication;
import com.example.job_tracker.jobapplication.enums.*;

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
        Instant createdAt,
        Instant updatedAt) {

    // Zakłada, że company jest już załadowane (EntityGraph w repozytorium), bo open-in-view jest wyłączone.
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
                application.getCreatedAt(),
                application.getUpdatedAt());
    }
}
