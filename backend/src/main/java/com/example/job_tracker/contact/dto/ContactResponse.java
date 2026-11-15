package com.example.job_tracker.contact.dto;


import com.example.job_tracker.contact.Contact;
import java.time.Instant;

public record ContactResponse(
        Long id,
        Long companyId,
        String companyName,
        String firstName,
        String lastName,
        String position,
        String email,
        String phone,
        String linkedinUrl,
        String notes,
        Instant createdAt,
        Instant updatedAt) {

    // Zakłada, że company jest już załadowane (EntityGraph), bo open-in-view jest wyłączone.
    public static ContactResponse from(Contact contact) {
        return new ContactResponse(
                contact.getId(),
                contact.getCompany().getId(),
                contact.getCompany().getName(),
                contact.getFirstName(),
                contact.getLastName(),
                contact.getPosition(),
                contact.getEmail(),
                contact.getPhone(),
                contact.getLinkedinUrl(),
                contact.getNotes(),
                contact.getCreatedAt(),
                contact.getUpdatedAt());
    }
}
