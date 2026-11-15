package com.example.job_tracker.contact;

import java.util.List;

import com.example.job_tracker.common.exceptions.ResourceNotFoundException;
import com.example.job_tracker.company.Company;
import com.example.job_tracker.company.CompanyRepository;
import com.example.job_tracker.contact.dto.ContactRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContactService {

    private static final Sort DEFAULT_SORT = Sort.by("lastName", "firstName");

    private final ContactRepository contactRepository;
    private final CompanyRepository companyRepository;

    @Transactional
    public Contact create(ContactRequest request) {
        Contact contact = new Contact(findCompany(request.companyId()), request.firstName().trim());
        apply(contact, request);
        return contactRepository.save(contact);
    }

    @Transactional(readOnly = true)
    public Contact get(Long id) {
        return contactRepository.findWithCompanyById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact", id));
    }

    @Transactional(readOnly = true)
    public List<Contact> list(Long companyId) {
        return companyId == null
                ? contactRepository.findAllBy(DEFAULT_SORT)
                : contactRepository.findByCompanyId(companyId, DEFAULT_SORT);
    }

    @Transactional
    public Contact update(Long id, ContactRequest request) {
        Contact contact = get(id);
        // Kontakt można przenieść do innej firmy, np. rekruter zmienił pracodawcę.
        if (!contact.getCompany().getId().equals(request.companyId())) {
            contact.setCompany(findCompany(request.companyId()));
        }
        contact.setFirstName(request.firstName().trim());
        apply(contact, request);
        return contact;
    }

    /**
     * Usunięcie kontaktu usuwa też jego powiązania z wpisami historii (ON DELETE CASCADE w bazie).
     */
    @Transactional
    public void delete(Long id) {
        contactRepository.delete(get(id));
    }

    private void apply(Contact contact, ContactRequest request) {
        contact.setLastName(request.lastName());
        contact.setPosition(request.position());
        contact.setEmail(request.email());
        contact.setPhone(request.phone());
        contact.setLinkedinUrl(request.linkedinUrl());
        contact.setNotes(request.notes());
    }

    private Company findCompany(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company", companyId));
    }
}
