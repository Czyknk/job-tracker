package com.example.job_tracker.jobapplication;

import com.example.job_tracker.common.BaseEntity;
import com.example.job_tracker.company.Company;
import com.example.job_tracker.document.Document;
import com.example.job_tracker.jobapplication.model.*;
import jakarta.persistence.*;
import org.hibernate.annotations.Formula;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "job_application")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobApplication extends BaseEntity {

    @ManyToMany
    @JoinTable(name = "application_document",
            joinColumns = @JoinColumn(name = "application_id"),
            inverseJoinColumns = @JoinColumn(name = "document_id"))
    @Setter(AccessLevel.NONE)
    private Set<Document> documents = new HashSet<>();
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false)
    private String position;

    @Column(length = 1000)
    private String offerUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private JobSource source = JobSource.OTHER;

    private String location;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private WorkMode workMode;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EmploymentType employmentType;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SeniorityLevel seniorityLevel;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "min", column = @Column(name = "salary_min", precision = 12, scale = 2)),
            @AttributeOverride(name = "max", column = @Column(name = "salary_max", precision = 12, scale = 2)),
            @AttributeOverride(name = "currency", column = @Column(name = "salary_currency", length = 3)),
            @AttributeOverride(name = "period", column = @Column(name = "salary_period", length = 10)),
            @AttributeOverride(name = "basis", column = @Column(name = "salary_basis", length = 10))
    })
    private SalaryRange salary;

    @Column(columnDefinition = "text")
    private String rawDescription;

    private LocalDate deadline;

    // Required; JobApplicationService.create defaults it to today from the Clock (app.timezone)
    @Column(nullable = false)
    private LocalDate dateFound;

    private LocalDate dateApplied;

    // Denormalized "current" status; the source of truth is the status history.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RecruitmentStatus currentStatus = RecruitmentStatus.FOUND;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(nullable = false)
    private boolean archived;

    // Next planned action (interview, task, follow-up). Optional; status changes never touch it.
    private Instant nextStepAt;

    @Column(length = 200)
    private String nextStepNote;

    @Version
    @Setter(AccessLevel.NONE)
    private Long version;

    // Derived, read-only: time of the most recent status history entry (FOUND included). Computed by a
    // correlated subquery in the same select (index on status_history(application_id, changed_at)), so it
    // can be filtered and sorted on without N+1 queries. It is read when the entity is loaded, so code that
    // changes the history and returns the entity must flush and refresh it.
    @Formula("(select max(sh.changed_at) from status_history sh where sh.application_id = id)")
    @Setter(AccessLevel.NONE)
    private Instant lastStatusChangedAt;
    
    
    public Set<Document> getDocuments() {
        return Collections.unmodifiableSet(documents);
    }

    public void attachDocument(Document document) {
        documents.add(document);
    }

    public boolean detachDocumentById(Long documentId) {
        return documents.removeIf(d -> Objects.equals(d.getId(), documentId));
    }
    
    public JobApplication(Company company, String position) {
        this.company = company;
        this.position = position;
    }

}
