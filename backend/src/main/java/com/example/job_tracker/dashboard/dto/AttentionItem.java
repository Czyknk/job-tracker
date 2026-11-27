package com.example.job_tracker.dashboard.dto;

import com.example.job_tracker.jobapplication.dto.JobApplicationListItem;

public record AttentionItem(AttentionReason reason, JobApplicationListItem application) {
}
