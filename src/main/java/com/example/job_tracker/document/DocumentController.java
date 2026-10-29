package com.example.job_tracker.document;

import com.example.job_tracker.document.dto.DocumentRequest;
import com.example.job_tracker.document.dto.DocumentResponse;
import com.example.job_tracker.document.dto.UpdateDocumentRequest;
import com.example.job_tracker.document.enums.DocumentType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.InputStreamResource;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "Documents")
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping
    @Operation(summary = "Register document metadata without uploading a file")
    public ResponseEntity<DocumentResponse> create(@Valid @RequestBody DocumentRequest request) {
        Document created = documentService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(DocumentResponse.from(created));
    }

    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a document file to object storage")
    public ResponseEntity<DocumentResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String name,
            @RequestParam DocumentType type,
            @RequestParam(required = false) LocalDate uploadDate) {
        Document created = documentService.upload(file, name, type, uploadDate);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .replacePath("/api/documents/{id}")
                .buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(DocumentResponse.from(created));
    }

    @GetMapping
    @Operation(summary = "List documents, newest first; archived ones are hidden unless includeArchived=true")
    public List<DocumentResponse> list(
            @RequestParam(required = false) DocumentType type,
            @RequestParam(defaultValue = "false") boolean includeArchived) {
        return documentService.list(type, includeArchived).stream().map(DocumentResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a document by id")
    public DocumentResponse get(@PathVariable Long id) {
        return DocumentResponse.from(documentService.get(id));
    }

    @GetMapping("/{id}/file")
    @Operation(summary = "Download a document file")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long id) {
        var storedObject = documentService.download(id);
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(storedObject.contentType());
        } catch (IllegalArgumentException ex) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(storedObject.contentLength())
                .body(new InputStreamResource(storedObject.content()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update name, type and archived flag")
    public DocumentResponse update(@PathVariable Long id, @Valid @RequestBody UpdateDocumentRequest request) {
        return DocumentResponse.from(documentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a document that is not attached to any application (otherwise 409: archive it)")
    public void delete(@PathVariable Long id) {
        documentService.delete(id);
    }
}
