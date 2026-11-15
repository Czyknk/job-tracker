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
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private CompanyService service;

    @Test
    void createsCompanyWithTrimmedName() {
        when(companyRepository.existsByNameIgnoreCase("Acme")).thenReturn(false);
        when(companyRepository.save(any(Company.class))).thenAnswer(inv -> inv.getArgument(0));

        Company created = service.create(new CompanyRequest("  Acme  ", "IT", "https://acme.example.com", null));

        assertThat(created.getName()).isEqualTo("Acme");
        assertThat(created.getIndustry()).isEqualTo("IT");
        assertThat(created.getWebsite()).isEqualTo("https://acme.example.com");
    }

    @Test
    void rejectsDuplicateName() {
        when(companyRepository.existsByNameIgnoreCase("Acme")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CompanyRequest("Acme", null, null, null)))
                .isInstanceOf(ConflictException.class);

        verify(companyRepository, never()).save(any());
    }

    @Test
    void getThrowsWhenCompanyDoesNotExist() {
        when(companyRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(42L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
