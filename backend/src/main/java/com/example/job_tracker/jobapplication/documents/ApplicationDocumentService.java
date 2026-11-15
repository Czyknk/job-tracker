package com.example.job_tracker.jobapplication.documents;

import java.util.Comparator;
import java.util.List;

import com.example.job_tracker.common.exceptions.ConflictException;
import com.example.job_tracker.common.exceptions.ResourceNotFoundException;
import com.example.job_tracker.document.Document;
import com.example.job_tracker.document.DocumentRepository;
import com.example.job_tracker.jobapplication.JobApplication;
import com.example.job_tracker.jobapplication.JobApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApplicationDocumentService {

    private final JobApplicationRepository applicationRepository;
    private final DocumentRepository documentRepository;

    @Transactional(readOnly = true)
    public List<Document> list(Long applicationId) {
        return findApplication(applicationId).getDocuments().stream()
                .sorted(Comparator.comparing(Document::getType).thenComparing(Document::getName))
                .toList();
    }

    /**
     * Idempotentne: ponowne podpięcie tego samego dokumentu nic nie zmienia.
     */
    @Transactional
    public void attach(Long applicationId, Long documentId) {
        JobApplication application = findApplication(applicationId);
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));

        if (application.getDocuments().contains(document)) {
            return;
        }
        if (document.isArchived()) {
            throw new ConflictException("Document " + documentId + " is archived and cannot be attached");
        }
        application.attachDocument(document);
    }

    /**
     * Idempotentne: odpięcie dokumentu, którego nie ma na aplikacji, nic nie robi.
     */
    @Transactional
    public void detach(Long applicationId, Long documentId) {
        findApplication(applicationId).detachDocumentById(documentId);
    }

    private JobApplication findApplication(Long applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("JobApplication", applicationId));
    }
}
