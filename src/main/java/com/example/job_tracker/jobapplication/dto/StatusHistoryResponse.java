package com.example.job_tracker.jobapplication.dto;

import com.example.job_tracker.contact.Contact;
import com.example.job_tracker.contact.dto.ContactResponse;
import com.example.job_tracker.jobapplication.StatusHistory;
import com.example.job_tracker.jobapplication.enums.RecruitmentStatus;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public record StatusHistoryResponse(Long id, RecruitmentStatus status, Instant changedAt,
                                    String note, List<ContactResponse> contacts) {
    public static StatusHistoryResponse from(StatusHistory entry) {
        return new StatusHistoryResponse(entry.getId(), entry.getStatus(), entry.getChangedAt(), entry.getNote(),
                entry.getContacts().stream()
                        .sorted(Comparator.comparing(Contact::getFirstName))
                        .map(ContactResponse::from).toList());
    }
}
