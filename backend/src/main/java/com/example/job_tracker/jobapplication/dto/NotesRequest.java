package com.example.job_tracker.jobapplication.dto;

import jakarta.validation.constraints.Size;

/**
 * Replaces the notes of an application; null or blank clears them.
 */
public record NotesRequest(@Size(max = 20000) String notes) {
}
