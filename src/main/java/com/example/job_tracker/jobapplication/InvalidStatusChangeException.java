package com.example.job_tracker.jobapplication;

import com.example.job_tracker.jobapplication.enums.RecruitmentStatus;

public class InvalidStatusChangeException extends RuntimeException {

    public InvalidStatusChangeException(RecruitmentStatus from, RecruitmentStatus to) {
        super("Cannot change status from " + from + " to " + to + ": " + from + " is a final status");
    }
}
