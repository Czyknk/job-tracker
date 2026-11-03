package com.example.job_tracker.jobapplication;

import com.example.job_tracker.common.PageResponse;
import com.example.job_tracker.jobapplication.dto.ChangeStatusRequest;
import com.example.job_tracker.jobapplication.dto.CreateJobApplicationRequest;
import com.example.job_tracker.jobapplication.dto.JobApplicationResponse;
import com.example.job_tracker.jobapplication.dto.StatusHistoryResponse;
import com.example.job_tracker.jobapplication.enums.RecruitmentStatus;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
@Tag(name = "Job applications")
public class JobApplicationController {

    private final JobApplicationService service;

    @PostMapping
    @Operation(summary = "Create a job application (the first history entry, FOUND, is created automatically)")
    public ResponseEntity<JobApplicationResponse> create(@Valid @RequestBody CreateJobApplicationRequest request) {
        JobApplication created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(JobApplicationResponse.from(created));
    }

    @GetMapping
    @Operation(summary = "List applications (paged), optionally filtered by status and/or company",
            description = "Sortable by: dateFound, dateApplied, deadline, position, currentStatus, createdAt. "
                    + "Example: sort=deadline,asc")
    public PageResponse<JobApplicationResponse> list(
            @RequestParam(required = false) RecruitmentStatus status,
            @RequestParam(required = false) Long companyId,
            @ParameterObject @PageableDefault(size = 20, sort = "dateFound", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return PageResponse.from(service.search(status, companyId, pageable), JobApplicationResponse::from);
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

    @GetMapping("/{id}/history")
    @Operation(summary = "Get the status history of an application, oldest first")
    public List<StatusHistoryResponse> history(@PathVariable Long id) {
        return service.getHistory(id).stream().map(StatusHistoryResponse::from).toList();
    }
}
