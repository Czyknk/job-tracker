package com.example.job_tracker.company.dto;

import com.example.job_tracker.company.Company;

import java.time.Instant;

public record CompanyResponse(
        Long id,
        String name,
        String industry,
        String website,
        String notes,
        Instant createdAt,
        Instant updatedAt) {

    public static CompanyResponse from(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getIndustry(),
                company.getWebsite(),
                company.getNotes(),
                company.getCreatedAt(),
                company.getUpdatedAt());
    }
}