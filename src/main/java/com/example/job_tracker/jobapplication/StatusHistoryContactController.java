package com.example.job_tracker.jobapplication;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/applications/{applicationId}/history/{entryId}/contacts/{contactId}")
@RequiredArgsConstructor
@Tag(name = "Status history contacts")
public class StatusHistoryContactController {

    private final StatusHistoryContactService service;

    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Attach a contact to a status history entry (idempotent)")
    public void attach(@PathVariable Long applicationId, @PathVariable Long entryId,
                       @PathVariable Long contactId) {
        service.attach(applicationId, entryId, contactId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Detach a contact from a status history entry (idempotent)")
    public void detach(@PathVariable Long applicationId, @PathVariable Long entryId,
                       @PathVariable Long contactId) {
        service.detach(applicationId, entryId, contactId);
    }
}
