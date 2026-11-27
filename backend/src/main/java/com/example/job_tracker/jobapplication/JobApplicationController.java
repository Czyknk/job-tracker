package com.example.job_tracker.jobapplication;

import com.example.job_tracker.common.PageResponse;
import com.example.job_tracker.jobapplication.dto.*;
import com.example.job_tracker.jobapplication.history.dto.ChangeStatusRequest;
import com.example.job_tracker.jobapplication.history.dto.StatusHistoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
@Tag(name = "Job applications")
public class JobApplicationController {

    private final JobApplicationService service;
    private final ApplicationApplyService applyService;

    @PostMapping
    @Operation(summary = "Create a job application (the first history entry, FOUND, is created automatically)")
    public ResponseEntity<JobApplicationResponse> create(@Valid @RequestBody CreateJobApplicationRequest request) {
        JobApplication created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(JobApplicationResponse.from(created));
    }

    @GetMapping
    public PageResponse<JobApplicationListItem> list(
            @Valid @ParameterObject ApplicationFilter filter,
            @ParameterObject @PageableDefault(size = 20, sort = "dateFound", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return PageResponse.from(service.search(filter, pageable), JobApplicationListItem::from);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an application by id")
    public JobApplicationResponse get(@PathVariable Long id) {
        return JobApplicationResponse.from(service.get(id));
    }

    @PostMapping("/{id}/status")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Change the recruitment status (adds a history entry and updates currentStatus)",
            description = "ACCEPTED, REJECTED and WITHDRAWN are final: further changes return 409.")
    public StatusHistoryResponse changeStatus(@PathVariable Long id,
                                              @Valid @RequestBody ChangeStatusRequest request) {
        return StatusHistoryResponse.from(
                service.changeStatus(id, request.status(), request.changedAt(), request.note(), request.contactIds()));
    }

    @PostMapping("/{id}/apply")
    @Operation(summary = "Mark an application as applied in one step (APPLIED entry, dateApplied, documents)",
            description = "Only an application in FOUND can be applied; otherwise 409. appliedAt defaults to now "
                    + "and must not be before dateFound (400). Documents are attached idempotently; a missing "
                    + "document gives 404 and an archived one 409, and then nothing is changed. "
                    + "The body is optional.")
    public JobApplicationResponse apply(@PathVariable Long id,
                                        @Valid @RequestBody(required = false) ApplyRequest request) {
        return JobApplicationResponse.from(applyService.apply(id, request));
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Get the status history of an application, oldest first")
    public List<StatusHistoryResponse> history(@PathVariable Long id) {
        return service.getHistory(id).stream().map(StatusHistoryResponse::from).toList();
    }

    @PutMapping("/{id}/notes")
    @Operation(summary = "Replace the notes of an application (blank clears them)")
    public JobApplicationResponse updateNotes(@PathVariable Long id, @Valid @RequestBody NotesRequest request) {
        return JobApplicationResponse.from(service.updateNotes(id, request.notes()));
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Archive an application (hidden from the default list)")
    public JobApplicationResponse archive(@PathVariable Long id) {
        return JobApplicationResponse.from(service.setArchived(id, true));
    }

    @PostMapping("/{id}/unarchive")
    @Operation(summary = "Restore an archived application")
    public JobApplicationResponse unarchive(@PathVariable Long id) {
        return JobApplicationResponse.from(service.setArchived(id, false));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace the editable fields (the status changes only via /status)")
    public JobApplicationResponse update(@PathVariable Long id,
                                         @Valid @RequestBody UpdateJobApplicationRequest request) {
        return JobApplicationResponse.from(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an application with its history, summaries and document links")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
