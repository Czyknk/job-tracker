package com.example.job_tracker.jobapplication.dto;

import com.example.job_tracker.jobapplication.model.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.Set;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Query parameters of the application list. Every parameter is optional and all given parameters are combined
 * with AND. Day-based filters use the application time zone (app.timezone).
 */
public record ApplicationFilter(
        @Schema(description = "Current status; repeat the parameter for several (status=APPLIED&status=HR_SCREEN)")
        Set<RecruitmentStatus> status,
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
        @Schema(description = "ACTIVE = not archived, ARCHIVED or ALL. When neither this nor `active` is given, "
                + "ACTIVE is used; when only `active` is given, there is no extra archive filter")
        ArchiveScope archived,
        @Schema(description = "true: not archived and the process is still open, i.e. the current status is not "
                + "ACCEPTED, REJECTED, WITHDRAWN or GHOSTED. GHOSTED counts as finished here, although it is not a "
                + "final status in the history (a late reply can still move it on). false: archived or finished")
        Boolean active,
        @Schema(description = "Applications waiting for the employer: an open status other than FOUND, not "
                + "archived, and the last status change at least this many calendar days ago")
        @Min(1) Integer staleDays,
        @Schema(description = "Applications with a deadline between today and today + N days (inclusive)")
        @Min(0) Integer deadlineWithinDays) {
}
