package com.example.job_tracker.document.dto;

import com.example.job_tracker.document.enums.DocumentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record DocumentRequest(
        @Schema(example = "CV_Java_Backend_v1")
        @NotBlank @Size(max = 255) 
        String name,
        
        @NotNull 
        DocumentType type,
        
        @Schema(description = "Defaults to today when omitted")
        LocalDate uploadDate) {
}
