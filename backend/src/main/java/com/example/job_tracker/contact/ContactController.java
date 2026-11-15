package com.example.job_tracker.contact;

import com.example.job_tracker.contact.dto.ContactRequest;
import com.example.job_tracker.contact.dto.ContactResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
@Tag(name = "Contacts")
public class ContactController {

    private final ContactService contactService;

    @PostMapping
    @Operation(summary = "Create a contact person assigned to a company")
    public ResponseEntity<ContactResponse> create(@Valid @RequestBody ContactRequest request) {
        Contact created = contactService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(ContactResponse.from(created));
    }

    @GetMapping
    @Operation(summary = "List contacts sorted by last name, optionally only those of one company")
    public List<ContactResponse> list(@RequestParam(required = false) Long companyId) {
        return contactService.list(companyId).stream().map(ContactResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a contact by id")
    public ContactResponse get(@PathVariable Long id) {
        return ContactResponse.from(contactService.get(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace the contact's data (the contact may be moved to another company)")
    public ContactResponse update(@PathVariable Long id, @Valid @RequestBody ContactRequest request) {
        return ContactResponse.from(contactService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a contact (also removes it from status history entries it took part in)")
    public void delete(@PathVariable Long id) {
        contactService.delete(id);
    }
}
