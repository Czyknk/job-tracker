package com.example.job_tracker.jobapplication;

import com.example.job_tracker.document.dto.DocumentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/applications/{applicationId}/documents")
@RequiredArgsConstructor
@Tag(name = "Application documents")
public class ApplicationDocumentController {

    private final ApplicationDocumentService service;

    @GetMapping
    @Operation(summary = "List documents attached to an application (CVs first, then by name)")
    public List<DocumentResponse> list(@PathVariable Long applicationId) {
        return service.list(applicationId).stream().map(DocumentResponse::from).toList();
    }

    @PutMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Attach a document to an application (idempotent; archived documents give 409)")
    public void attach(@PathVariable Long applicationId, @PathVariable Long documentId) {
        service.attach(applicationId, documentId);
    }

    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Detach a document from an application (idempotent)")
    public void detach(@PathVariable Long applicationId, @PathVariable Long documentId) {
        service.detach(applicationId, documentId);
    }
}
