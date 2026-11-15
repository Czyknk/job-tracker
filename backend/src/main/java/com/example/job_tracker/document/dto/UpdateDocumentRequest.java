package com.example.job_tracker.document.dto;

import com.example.job_tracker.document.enums.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Pełna zamiana edytowalnych pól (semantyka PUT).
 */
public record UpdateDocumentRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull DocumentType type,
        @NotNull Boolean archived) {
}
