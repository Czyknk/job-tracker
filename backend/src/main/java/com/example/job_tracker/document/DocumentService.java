package com.example.job_tracker.document;

import com.example.job_tracker.common.exceptions.ConflictException;
import com.example.job_tracker.common.exceptions.ResourceNotFoundException;
import com.example.job_tracker.document.dto.DocumentRequest;
import com.example.job_tracker.document.dto.UpdateDocumentRequest;
import com.example.job_tracker.document.enums.DocumentType;
import com.example.job_tracker.storage.ObjectStorage;
import java.io.IOException;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final ObjectStorage objectStorage;

    @Transactional
    public Document create(DocumentRequest request) {
        Document document = new Document(request.type(), request.name().trim());
        if (request.uploadDate() != null) {
            document.setUploadDate(request.uploadDate());
        }
        return documentRepository.save(document);
    }

    @Transactional(readOnly = true)
    public Document get(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", id));
    }

    @Transactional(readOnly = true)
    public List<Document> list(DocumentType type, boolean includeArchived) {
        Specification<Document> spec = (root, _, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (!includeArchived) {
                predicates.add(cb.isFalse(root.get("archived")));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return documentRepository.findAll(spec, Sort.by(Sort.Order.desc("uploadDate"), Sort.Order.desc("id")));
    }

    @Transactional
    public Document update(Long id, UpdateDocumentRequest request) {
        Document document = get(id);
        document.setName(request.name().trim());
        document.setType(request.type());
        document.setArchived(request.archived());
        return document;
    }

    /**
     * Dokument użyty w aplikacjach nie jest usuwany (zachowujemy informację, co wysłano), tylko archiwizowany.
     */
    @Transactional
    public void delete(Long id) {
        Document document = get(id);
        long usages = documentRepository.countApplicationsUsing(id);
        if (usages > 0) {
            throw new ConflictException(
                    "Document is attached to " + usages + " application(s); archive it instead of deleting");
        }
        if (document.hasFile()) {
            objectStorage.delete(document.getObjectKey());
        }
        documentRepository.delete(document);
    }

    @Transactional
    public Document upload(MultipartFile file, String name, DocumentType type, java.time.LocalDate uploadDate) {
        if (file.isEmpty()) {
            throw new com.example.job_tracker.common.exceptions.BadRequestException("File must not be empty");
        }
        String documentName = name == null || name.isBlank()
                ? file.getOriginalFilename()
                : name.trim();
        if (documentName == null || documentName.isBlank()) {
            throw new com.example.job_tracker.common.exceptions.BadRequestException("Document name is required");
        }

        String objectKey = "documents/" + UUID.randomUUID();
        String contentType = file.getContentType() == null
                ? "application/octet-stream"
                : file.getContentType();
        try (var input = file.getInputStream()) {
            objectStorage.put(objectKey, input, file.getSize(), contentType);
        } catch (IOException ex) {
            throw new com.example.job_tracker.storage.StorageException("Could not read uploaded file", ex);
        } catch (RuntimeException ex) {
            try {
                objectStorage.delete(objectKey);
            } catch (RuntimeException cleanupFailure) {
                ex.addSuppressed(cleanupFailure);
            }
            throw ex;
        }

        Document document = new Document(type, documentName);
        if (uploadDate != null) {
            document.setUploadDate(uploadDate);
        }
        document.setObjectKey(objectKey);
        document.setContentType(contentType);
        document.setFileSize(file.getSize());
        try {
            return documentRepository.save(document);
        } catch (RuntimeException ex) {
            objectStorage.delete(objectKey);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public ObjectStorage.StoredObject download(Long id) {
        Document document = get(id);
        if (!document.hasFile()) {
            throw new ResourceNotFoundException("File for document", id);
        }
        return objectStorage.get(document.getObjectKey());
    }
}
