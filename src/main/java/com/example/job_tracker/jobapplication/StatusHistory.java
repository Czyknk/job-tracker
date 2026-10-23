package com.example.job_tracker.jobapplication;

import com.example.job_tracker.common.BaseEntity;
import com.example.job_tracker.jobapplication.enums.RecruitmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Wpis w historii rekrutacji. Log zmian jest niezmienny.
 */
@Entity
@Table(name = "status_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StatusHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private JobApplication application;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RecruitmentStatus status;

    @Column(nullable = false)
    private Instant changedAt;

    @Column(columnDefinition = "text")
    private String note;

    public StatusHistory(JobApplication application, RecruitmentStatus status, Instant changedAt, String note) {
        this.application = application;
        this.status = status;
        this.changedAt = changedAt;
        this.note = note;
    }
}
