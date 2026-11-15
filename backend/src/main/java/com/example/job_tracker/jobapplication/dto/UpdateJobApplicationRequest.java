package com.example.job_tracker.jobapplication.dto;

import com.example.job_tracker.jobapplication.model.EmploymentType;
import com.example.job_tracker.jobapplication.model.JobSource;
import com.example.job_tracker.jobapplication.model.SeniorityLevel;
import com.example.job_tracker.jobapplication.model.WorkMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Pełna zamiana edytowalnych pól (semantyka PUT): pominięte pola opcjonalne zostaną wyczyszczone.
 * Status zmienia się osobnym endpointem /status, bo każda zmiana trafia do historii.
 */
public record UpdateJobApplicationRequest(
        @Schema(example = "1")
        @NotNull Long companyId,
        @Schema(example = "Junior Java Developer")
        @NotBlank @Size(max = 255) String position,
        @Size(max = 1000) String offerUrl,
        @NotNull JobSource source,
        @Size(max = 255) String location,
        WorkMode workMode,
        EmploymentType employmentType,
        SeniorityLevel seniorityLevel,
        @Valid SalaryRangeDto salary,
        String rawDescription,
        LocalDate deadline,
        @NotNull LocalDate dateFound,
        LocalDate dateApplied) {
}
