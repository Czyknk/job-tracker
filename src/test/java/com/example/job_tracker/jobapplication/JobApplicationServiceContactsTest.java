package com.example.job_tracker.jobapplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.example.job_tracker.common.exceptions.ResourceNotFoundException;
import com.example.job_tracker.company.Company;
import com.example.job_tracker.company.CompanyRepository;
import com.example.job_tracker.contact.Contact;
import com.example.job_tracker.contact.ContactRepository;
import com.example.job_tracker.jobapplication.enums.RecruitmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JobApplicationServiceContactsTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-10-28T10:00:00Z"), ZoneOffset.UTC);

    @Mock
    private JobApplicationRepository applicationRepository;

    @Mock
    private StatusHistoryRepository statusHistoryRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private ContactRepository contactRepository;

    private JobApplicationService service;
    private JobApplication application;
    private Company company;

    @BeforeEach
    void setUp() {
        service = new JobApplicationService(
                applicationRepository, statusHistoryRepository, companyRepository, contactRepository, FIXED_CLOCK);
        company = new Company("Acme");
        application = new JobApplication(company, "Junior Java Developer");
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
    }

    @Test
    void attachesContactsToNewHistoryEntry() {
        Contact anna = contact("Anna", 10L);
        Contact piotr = contact("Piotr", 11L);
        when(contactRepository.findWithCompanyByIdIn(Set.of(10L, 11L))).thenReturn(List.of(anna, piotr));
        when(statusHistoryRepository.save(any(StatusHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        StatusHistory entry = service.changeStatus(
                1L, RecruitmentStatus.TECH_INTERVIEW, null, "Tech interview", Set.of(10L, 11L));

        assertThat(entry.getContacts()).containsExactlyInAnyOrder(anna, piotr);
        assertThat(application.getCurrentStatus()).isEqualTo(RecruitmentStatus.TECH_INTERVIEW);
    }

    @Test
    void unknownContactFailsBeforeAnythingIsChanged() {
        Contact anna = contact("Anna", 10L);
        when(contactRepository.findWithCompanyByIdIn(Set.of(10L, 99L))).thenReturn(List.of(anna));

        assertThatThrownBy(() -> service.changeStatus(
                1L, RecruitmentStatus.HR_SCREEN, null, null, Set.of(10L, 99L)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(statusHistoryRepository, never()).save(any());
        assertThat(application.getCurrentStatus()).isEqualTo(RecruitmentStatus.FOUND);
    }

    private Contact contact(String firstName, Long id) {
        Contact contact = new Contact(company, firstName);
        ReflectionTestUtils.setField(contact, "id", id);
        return contact;
    }
}
