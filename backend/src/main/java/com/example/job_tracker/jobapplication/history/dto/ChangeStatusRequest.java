package com.example.job_tracker.jobapplication.history.dto;

import com.example.job_tracker.jobapplication.model.RecruitmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;

public record ChangeStatusRequest(
        @NotNull RecruitmentStatus status,
        @Schema(description = "When the change happened; defaults to now", example = "2026-10-22T10:00:00Z")
        @PastOrPresent Instant changedAt,
        @Schema(example = "HR call scheduled for Monday")
        @Size(max = 2000) String note,
        @Schema(description = "Ids of contacts who took part in this stage, e.g. interviewers")
        @Size(max = 20) Set<Long> contactIds) {
}
