package com.example.job_tracker.summary;

import com.example.job_tracker.summary.dto.OfferSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/applications/{applicationId}/summaries")
@RequiredArgsConstructor
@Tag(name = "Offer summaries (AI)")
public class OfferSummaryController {

    private final OfferSummaryService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Generate an AI summary of the application's rawDescription",
            description = "Calls the configured LLM. Every call stores a new summary; the newest one is the "
                    + "current one. 400 = no rawDescription, 502 = provider error (e.g. rate limit), "
                    + "503 = provider not configured (missing API key).")
    public OfferSummaryResponse generate(@PathVariable Long applicationId) {
        return OfferSummaryResponse.from(service.generate(applicationId));
    }

    @GetMapping
    @Operation(summary = "List all generated summaries of an application, newest first")
    public List<OfferSummaryResponse> list(@PathVariable Long applicationId) {
        return service.list(applicationId).stream().map(OfferSummaryResponse::from).toList();
    }

    @GetMapping("/latest")
    @Operation(summary = "Get the newest summary (404 if none was generated yet)")
    public OfferSummaryResponse latest(@PathVariable Long applicationId) {
        return OfferSummaryResponse.from(service.getLatest(applicationId));
    }
}
