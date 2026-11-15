package com.example.job_tracker.jobapplication.model;

public enum RecruitmentStatus {

    FOUND,
    APPLIED,
    HR_SCREEN,
    TECH_TASK,
    TECH_INTERVIEW,
    FINAL_INTERVIEW,
    OFFER_RECEIVED,
    ACCEPTED,
    REJECTED,
    WITHDRAWN,
    GHOSTED;

    public boolean isTerminal() {
        return this == ACCEPTED || this == REJECTED || this == WITHDRAWN;
    }
}
