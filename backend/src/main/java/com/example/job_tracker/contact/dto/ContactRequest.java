package com.example.job_tracker.contact.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Używane zarówno do tworzenia, jak i do pełnej aktualizacji (PUT).
 */
public record ContactRequest(
        @Schema(description = "Company (or recruitment agency) the person works for", example = "1")
        @NotNull Long companyId,
        @Schema(example = "Anna")
        @NotBlank @Size(max = 100) String firstName,
        @Schema(example = "Nowak")
        @Size(max = 100) String lastName,
        @Schema(example = "Tech Recruiter")
        @Size(max = 255) String position,
        @Schema(example = "anna.nowak@example.com")
        @Email @Size(max = 255) String email,
        @Size(max = 50) String phone,
        @Size(max = 500) String linkedinUrl,
        String notes) {
}
