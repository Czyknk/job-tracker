package com.example.job_tracker.jobapplication.dto;

import com.example.job_tracker.jobapplication.model.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Query parameters of the application list. Every parameter is optional.
 */
public record ApplicationFilter(
        RecruitmentStatus status,
        Long companyId,
        @Schema(description = "Words to look for in position, company, location, description and notes "
                + "(case-insensitive, every word must match somewhere)")
        String q,
        JobSource source,
        WorkMode workMode,
        EmploymentType employmentType,
        SeniorityLevel seniorityLevel,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate foundFrom,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate foundTo,
        @Schema(description = "Applications with a deadline on or before this date")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deadlineBefore,
        @Schema(description = "ACTIVE (default), ARCHIVED or ALL")
        ArchiveScope archived) {

    public ApplicationFilter {
        if (archived == null) {
            archived = ArchiveScope.ACTIVE;
        }
    }
}
