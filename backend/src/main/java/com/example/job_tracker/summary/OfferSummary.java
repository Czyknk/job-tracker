package com.example.job_tracker.summary;

import com.example.job_tracker.common.BaseEntity;
import com.example.job_tracker.jobapplication.JobApplication;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Streszczenie oferty wygenerowane przez AI. Niezmienne: kolejne generowania dodają nowe wpisy,
 * a "aktualne" to najnowszy.
 */
@Entity
@Table(name = "offer_summary")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OfferSummary extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private JobApplication application;

    @Column(nullable = false, columnDefinition = "text")
    private String summaryText;

    @Column(nullable = false)
    private Instant generatedAt;

    @Column(length = 100)
    private String modelUsed;

    public OfferSummary(JobApplication application, String summaryText, Instant generatedAt, String modelUsed) {
        this.application = application;
        this.summaryText = summaryText;
        this.generatedAt = generatedAt;
        this.modelUsed = modelUsed;
    }
}
