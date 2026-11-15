package com.example.job_tracker.summary.dto;

import com.example.job_tracker.summary.OfferSummary;

import java.time.Instant;

public record OfferSummaryResponse(Long id, String summaryText, Instant generatedAt, String modelUsed) {

    public static OfferSummaryResponse from(OfferSummary summary) {
        return new OfferSummaryResponse(
                summary.getId(), summary.getSummaryText(), summary.getGeneratedAt(), summary.getModelUsed());
    }
}
