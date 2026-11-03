package com.example.job_tracker.contact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.example.job_tracker.common.exceptions.ResourceNotFoundException;
import com.example.job_tracker.company.Company;
import com.example.job_tracker.company.CompanyRepository;
import com.example.job_tracker.contact.dto.ContactRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private ContactService service;

    @Test
    void createsContactForExistingCompany() {
        Company company = new Company("Acme");
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(contactRepository.save(any(Contact.class))).thenAnswer(inv -> inv.getArgument(0));

        Contact created = service.create(new ContactRequest(
                1L, " Anna ", "Nowak", "Tech Recruiter", "anna@acme.example", "+48 123 456 789", null, null));

        assertThat(created.getFirstName()).isEqualTo("Anna");
        assertThat(created.getLastName()).isEqualTo("Nowak");
        assertThat(created.getCompany()).isSameAs(company);
        assertThat(created.getEmail()).isEqualTo("anna@acme.example");
    }

    @Test
    void createFailsForUnknownCompany() {
        when(companyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(
                new ContactRequest(99L, "Anna", null, null, null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(contactRepository, never()).save(any());
    }

    @Test
    void updateMovesContactToAnotherCompany() {
        Company oldCompany = new Company("Old");
        ReflectionTestUtils.setField(oldCompany, "id", 1L);
        Company newCompany = new Company("New");
        ReflectionTestUtils.setField(newCompany, "id", 2L);
        Contact contact = new Contact(oldCompany, "Anna");
        when(contactRepository.findWithCompanyById(5L)).thenReturn(Optional.of(contact));
        when(companyRepository.findById(2L)).thenReturn(Optional.of(newCompany));

        service.update(5L, new ContactRequest(2L, "Anna", "Kowalska", "Recruiter", null, null, null, null));

        assertThat(contact.getCompany()).isSameAs(newCompany);
        assertThat(contact.getLastName()).isEqualTo("Kowalska");
    }
}
