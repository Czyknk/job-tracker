package com.example.job_tracker.jobapplication.dto;

import com.example.job_tracker.jobapplication.enums.RecruitmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record ChangeStatusRequest(
        @NotNull RecruitmentStatus status,
        @Schema(description = "When the change happened; defaults to now", example = "2026-10-22T10:00:00Z")
        @PastOrPresent Instant changedAt,
        @Schema(example = "HR call scheduled for Monday")
        @Size(max = 2000) String note) {
}
