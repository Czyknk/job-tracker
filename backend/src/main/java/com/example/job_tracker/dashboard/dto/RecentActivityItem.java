package com.example.job_tracker.dashboard.dto;

import java.time.Instant;

import com.example.job_tracker.jobapplication.history.StatusHistoryRepository;
import com.example.job_tracker.jobapplication.model.RecruitmentStatus;

public record RecentActivityItem(
        Long entryId,
        Long applicationId,
        String position,
        String companyName,
        RecruitmentStatus status,
        Instant changedAt) {

    public static RecentActivityItem from(StatusHistoryRepository.RecentActivity activity) {
        return new RecentActivityItem(activity.getEntryId(), activity.getApplicationId(), activity.getPosition(),
                activity.getCompanyName(), activity.getStatus(), activity.getChangedAt());
    }
}
