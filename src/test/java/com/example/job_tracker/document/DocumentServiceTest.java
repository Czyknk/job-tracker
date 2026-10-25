package com.example.job_tracker.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import com.example.job_tracker.common.exceptions.ConflictException;
import com.example.job_tracker.common.exceptions.ResourceNotFoundException;
import com.example.job_tracker.document.dto.DocumentRequest;
import com.example.job_tracker.document.dto.UpdateDocumentRequest;
import com.example.job_tracker.document.enums.DocumentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @InjectMocks
    private DocumentService service;

    @Test
    void createsDocumentWithTrimmedNameAndGivenDate() {
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        Document created = service.create(
                new DocumentRequest("  CV_v1  ", DocumentType.CV, LocalDate.of(2026, 10, 1)));

        assertThat(created.getName()).isEqualTo("CV_v1");
        assertThat(created.getType()).isEqualTo(DocumentType.CV);
        assertThat(created.getUploadDate()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(created.isArchived()).isFalse();
    }

    @Test
    void refusesToDeleteDocumentAttachedToApplications() {
        Document document = new Document(DocumentType.CV, "CV_v1");
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentRepository.countApplicationsUsing(1L)).thenReturn(3L);

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(ConflictException.class);

        verify(documentRepository, never()).delete((Document) any());
    }

    @Test
    void deletesUnusedDocument() {
        Document document = new Document(DocumentType.OTHER, "Certificate");
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentRepository.countApplicationsUsing(1L)).thenReturn(0L);

        service.delete(1L);

        verify(documentRepository).delete(document);
    }

    @Test
    void updateChangesNameTypeAndArchivedFlag() {
        Document document = new Document(DocumentType.CV, "CV_v1");
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));

        service.update(1L, new UpdateDocumentRequest(" CV_v1_old ", DocumentType.OTHER, true));

        assertThat(document.getName()).isEqualTo("CV_v1_old");
        assertThat(document.getType()).isEqualTo(DocumentType.OTHER);
        assertThat(document.isArchived()).isTrue();
    }

    @Test
    void getThrowsWhenDocumentDoesNotExist() {
        when(documentRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(9L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
