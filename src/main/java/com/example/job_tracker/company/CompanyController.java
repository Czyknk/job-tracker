package com.example.job_tracker.company;

import com.example.job_tracker.company.dto.CompanyRequest;
import com.example.job_tracker.company.dto.CompanyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
@Tag(name = "Companies")
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping
    @Operation(summary = "Create a company (name must be unique, case-insensitive)")
    public ResponseEntity<CompanyResponse> create(@Valid @RequestBody CompanyRequest request) {
        Company created = companyService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(CompanyResponse.from(created));
    }

    @GetMapping
    @Operation(summary = "List all companies sorted by name")
    public List<CompanyResponse> list() {
        return companyService.list().stream().map(CompanyResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a company by id")
    public CompanyResponse get(@PathVariable Long id) {
        return CompanyResponse.from(companyService.get(id));
    }
}
