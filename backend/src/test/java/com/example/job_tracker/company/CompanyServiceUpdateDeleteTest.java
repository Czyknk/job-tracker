package com.example.job_tracker.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.example.job_tracker.common.exceptions.ConflictException;
import com.example.job_tracker.common.exceptions.ResourceNotFoundException;
import com.example.job_tracker.company.dto.CompanyRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompanyServiceUpdateDeleteTest {

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private CompanyService service;

    @Test
    void updateReplacesFieldsAndTrimsName() {
        Company company = new Company("Old name");
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(companyRepository.existsByNameIgnoreCaseAndIdNot("New name", 1L)).thenReturn(false);

        service.update(1L, new CompanyRequest(" New name ", "Fintech", "https://new.example.com", "notes"));

        assertThat(company.getName()).isEqualTo("New name");
        assertThat(company.getIndustry()).isEqualTo("Fintech");
        assertThat(company.getWebsite()).isEqualTo("https://new.example.com");
        assertThat(company.getNotes()).isEqualTo("notes");
    }

    @Test
    void updateRejectsNameUsedByAnotherCompany() {
        Company company = new Company("Old name");
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(companyRepository.existsByNameIgnoreCaseAndIdNot("Taken", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, new CompanyRequest("Taken", null, null, null)))
                .isInstanceOf(ConflictException.class);

        assertThat(company.getName()).isEqualTo("Old name");
    }

    @Test
    void deleteIsRefusedWhenCompanyHasApplications() {
        Company company = new Company("Acme");
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(companyRepository.countApplications(1L)).thenReturn(2L);

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(ConflictException.class);

        verify(companyRepository, never()).delete(any());
    }

    @Test
    void deleteRemovesCompanyWithoutApplications() {
        Company company = new Company("Acme");
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(companyRepository.countApplications(1L)).thenReturn(0L);

        service.delete(1L);

        verify(companyRepository).delete(company);
    }

    @Test
    void deleteThrowsWhenCompanyDoesNotExist() {
        when(companyRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(9L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
