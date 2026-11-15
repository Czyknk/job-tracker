package com.example.job_tracker.document.dto;

import com.example.job_tracker.document.Document;
import com.example.job_tracker.document.enums.DocumentType;

import java.time.Instant;
import java.time.LocalDate;

public record DocumentResponse(
        Long id,
        DocumentType type,
        String name,
        LocalDate uploadDate,
        boolean archived,
        boolean hasFile,
        String contentType,
        Long fileSize,
        Instant createdAt,
        Instant updatedAt) {

    public static DocumentResponse from(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getType(),
                document.getName(),
                document.getUploadDate(),
                document.isArchived(),
                document.hasFile(),
                document.getContentType(),
                document.getFileSize(),
                document.getCreatedAt(),
                document.getUpdatedAt());
    }
}
