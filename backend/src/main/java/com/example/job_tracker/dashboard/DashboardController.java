package com.example.job_tracker.dashboard;

import com.example.job_tracker.dashboard.dto.DashboardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard")
public class DashboardController {

    private final DashboardService service;

    @GetMapping
    @Operation(summary = "Data for the \"Today\" view: active counts, applications needing attention, "
            + "upcoming next steps and recent activity (read-only, available to every role)")
    public DashboardResponse get() {
        return service.get();
    }
}
