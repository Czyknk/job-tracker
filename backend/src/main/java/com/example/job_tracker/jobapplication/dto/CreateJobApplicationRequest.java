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

public record CreateJobApplicationRequest(
        @Schema(description = "Id of an existing company", example = "1")
        @NotNull Long companyId,
        @Schema(example = "Junior Java Developer")
        @NotBlank @Size(max = 255) String position,
        @Size(max = 1000) String offerUrl,
        @Schema(description = "Defaults to OTHER when omitted")
        JobSource source,
        @Size(max = 255) String location,
        WorkMode workMode,
        EmploymentType employmentType,
        SeniorityLevel seniorityLevel,
        @Valid SalaryRangeDto salary,
        @Schema(description = "Full pasted job description")
        String rawDescription,
        LocalDate deadline,
        @Schema(description = "Defaults to today when omitted")
        LocalDate dateFound,
        LocalDate dateApplied) {
}
