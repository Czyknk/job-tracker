package com.example.job_tracker.entity;

import com.example.job_tracker.entity.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "job_application")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobApplication extends BaseAuditableEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Wymuszony FetchType.LAZY (dobre praktyki dla relacji *-to-One)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false)
    private String position;

    @Column(name = "offer_url", length = 1000)
    private String offerUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ApplicationSource source = ApplicationSource.OTHER;

    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_mode", length = 20)
    private WorkMode workMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", length = 20)
    private EmploymentType employmentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "seniority_level", length = 20)
    private SeniorityLevel seniorityLevel;

    @Embedded
    private SalaryRange salaryRange;

    @Column(name = "raw_description", columnDefinition = "TEXT")
    private String rawDescription;

    private LocalDate deadline;

    @Column(name = "date_found", nullable = false)
    @Builder.Default
    private LocalDate dateFound = LocalDate.now();

    @Column(name = "date_applied")
    private LocalDate dateApplied;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false, length = 30)
    @Builder.Default
    private ApplicationStatus currentStatus = ApplicationStatus.FOUND;
    
}
