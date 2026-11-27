package com.example.job_tracker.dashboard.dto;

import java.util.List;
import java.util.Map;

import com.example.job_tracker.jobapplication.dto.JobApplicationListItem;
import com.example.job_tracker.jobapplication.model.RecruitmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Everything the "Today" view needs in one response.
 */
public record DashboardResponse(
        @Schema(description = "Number of active applications (not archived, process still open) per status. "
                + "Every open status is present, in pipeline order, with 0 when there are none")
        Map<RecruitmentStatus, Long> activeByStatus,
        @Schema(description = "Active applications that need an action, grouped by reason in the order "
                + "DEADLINE_SOON, NO_RESPONSE, NOT_APPLIED_YET. An application can appear under several reasons")
        List<AttentionItem> needsAttention,
        @Schema(description = "Active applications with a next step from the start of today on, soonest first")
        List<JobApplicationListItem> upcoming,
        @Schema(description = "The latest status history entries, newest first")
        List<RecentActivityItem> recentActivity) {
}
