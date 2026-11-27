package com.example.job_tracker.jobapplication.dto;

import java.time.Instant;
import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

/**
 * Marks an application as applied in one step. Every field is optional; an empty body means "applied now".
 */
public record ApplyRequest(
        @Schema(description = "When the application was sent; defaults to now", example = "2026-11-20T09:30:00Z")
        @PastOrPresent Instant appliedAt,
        @Schema(description = "Ids of documents (CV, cover letter) that were sent; attaching is idempotent")
        @Size(max = 20) Set<Long> documentIds,
        @Schema(description = "Note stored on the APPLIED history entry", example = "Sent via the company website")
        @Size(max = 2000) String note) {
}
