package com.example.job_tracker.jobapplication.history;

import com.example.job_tracker.common.exceptions.ResourceNotFoundException;
import com.example.job_tracker.contact.Contact;
import com.example.job_tracker.contact.ContactRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Zarządzanie osobami powiązanymi z konkretnym wpisem historii (np. kto prowadził rozmowę techniczną).
 */
@Service
@RequiredArgsConstructor
public class StatusHistoryContactService {

    private final StatusHistoryRepository statusHistoryRepository;
    private final ContactRepository contactRepository;

    /**
     * Idempotentne: ponowne dodanie tego samego kontaktu nic nie zmienia.
     */
    @Transactional
    public void attach(Long applicationId, Long entryId, Long contactId) {
        StatusHistory entry = findEntry(applicationId, entryId);
        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact", contactId));
        entry.addContact(contact);
    }

    /**
     * Idempotentne: odpięcie kontaktu, którego nie ma we wpisie, nic nie robi.
     */
    @Transactional
    public void detach(Long applicationId, Long entryId, Long contactId) {
        findEntry(applicationId, entryId).removeContactById(contactId);
    }

    // Wpis musi należeć do wskazanej aplikacji, inaczej 404 (nie ujawniamy cudzych id).
    private StatusHistory findEntry(Long applicationId, Long entryId) {
        return statusHistoryRepository.findByIdAndApplicationId(entryId, applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("StatusHistory entry", entryId));
    }
}
