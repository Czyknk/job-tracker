package com.example.job_tracker.jobapplication.dto;

import com.example.job_tracker.jobapplication.StatusHistory;
import com.example.job_tracker.jobapplication.enums.RecruitmentStatus;

import java.time.Instant;

public record StatusHistoryResponse(Long id, RecruitmentStatus status, Instant changedAt, String note) {

    public static StatusHistoryResponse from(StatusHistory entry) {
        return new StatusHistoryResponse(entry.getId(), entry.getStatus(), entry.getChangedAt(), entry.getNote());
    }
}
