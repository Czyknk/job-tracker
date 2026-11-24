package com.example.job_tracker.jobapplication.history.dto;

import com.example.job_tracker.jobapplication.model.RecruitmentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Full replacement of the editable fields of a history entry (PUT semantics).
 */
public record CorrectHistoryEntryRequest(
        @NotNull RecruitmentStatus status,
        @NotNull @PastOrPresent Instant changedAt,
        @Size(max = 2000) String note) {
}
