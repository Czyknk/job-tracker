package com.example.job_tracker.jobapplication.history;

import com.example.job_tracker.jobapplication.history.dto.CorrectHistoryEntryRequest;
import com.example.job_tracker.jobapplication.history.dto.StatusHistoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/applications/{applicationId}/history/{entryId}")
@RequiredArgsConstructor
@Tag(name = "Status history corrections")
public class StatusHistoryCorrectionController {

    private final StatusHistoryCorrectionService service;

    @PutMapping
    @Operation(summary = "Correct a history entry (status, time, note)",
            description = "The current status of the application is recomputed from the latest entry. "
                    + "A final status (ACCEPTED, REJECTED, WITHDRAWN) must stay the last entry (409 otherwise).")
    public StatusHistoryResponse update(@PathVariable Long applicationId, @PathVariable Long entryId,
                                        @Valid @RequestBody CorrectHistoryEntryRequest request) {
        return StatusHistoryResponse.from(
                service.update(applicationId, entryId, request.status(), request.changedAt(), request.note()));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a history entry (\"undo\")",
            description = "The current status falls back to the latest remaining entry. "
                    + "The last remaining entry cannot be deleted (409).")
    public void delete(@PathVariable Long applicationId, @PathVariable Long entryId) {
        service.delete(applicationId, entryId);
    }
}
