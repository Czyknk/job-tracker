package com.example.job_tracker.jobapplication;

import com.example.job_tracker.common.BaseEntity;
import com.example.job_tracker.company.Company;
import com.example.job_tracker.jobapplication.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "job_application")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobApplication extends BaseEntity {

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


    public JobApplication(Company company, String position) {
        this.company = company;
        this.position = position;
    }

}
