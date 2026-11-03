package com.example.job_tracker.jobapplication;

import com.example.job_tracker.common.BaseEntity;
import com.example.job_tracker.company.Company;
import com.example.job_tracker.document.Document;
import com.example.job_tracker.jobapplication.model.*;
import jakarta.persistence.*;
import lombok.*;

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

    @Column(nullable = false)
    private LocalDate dateFound = LocalDate.now();

    private LocalDate dateApplied;

    // Zdenormalizowany "bieżący" status; źródłem prawdy jest historia statusów.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RecruitmentStatus currentStatus = RecruitmentStatus.FOUND;
    
    
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
