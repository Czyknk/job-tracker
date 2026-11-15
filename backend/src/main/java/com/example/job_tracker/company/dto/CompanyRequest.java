package com.example.job_tracker.company.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyRequest (
        @Schema(example = "Acme Sp. z o.o.")
        @NotBlank(message = "Company name cannot be blank")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name,
        
        @Schema(example = "Software house")
        @Size(max = 255, message = "Industry must not exceed 255 characters")
        String industry,
        
        @Schema(example = "https://acme.example.com")
        @Size(max = 500, message = "Website URL must not exceed 500 characters")
        String website,
        
        String notes){
    
}

