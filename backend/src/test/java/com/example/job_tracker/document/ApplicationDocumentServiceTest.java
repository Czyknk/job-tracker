package com.example.job_tracker.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.example.job_tracker.common.exceptions.ConflictException;
import com.example.job_tracker.company.Company;
import com.example.job_tracker.document.enums.DocumentType;
import com.example.job_tracker.jobapplication.documents.ApplicationDocumentService;
import com.example.job_tracker.jobapplication.JobApplication;
import com.example.job_tracker.jobapplication.JobApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplicationDocumentServiceTest {

    @Mock
    private JobApplicationRepository applicationRepository;

    @Mock
    private DocumentRepository documentRepository;

    @InjectMocks
    private ApplicationDocumentService service;

    private JobApplication application;

    @BeforeEach
    void setUp() {
        application = new JobApplication(new Company("Acme"), "Junior Java Developer");
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
    }

    @Test
    void attachAddsDocumentAndIsIdempotent() {
        Document cv = new Document(DocumentType.CV, "CV_v1");
        when(documentRepository.findById(2L)).thenReturn(Optional.of(cv));

        service.attach(1L, 2L);
        service.attach(1L, 2L);

        assertThat(application.getDocuments()).containsExactly(cv);
    }

    @Test
    void attachRejectsArchivedDocument() {
        Document archived = new Document(DocumentType.CV, "Old CV");
        archived.setArchived(true);
        when(documentRepository.findById(2L)).thenReturn(Optional.of(archived));

        assertThatThrownBy(() -> service.attach(1L, 2L)).isInstanceOf(ConflictException.class);

        assertThat(application.getDocuments()).isEmpty();
    }
}
