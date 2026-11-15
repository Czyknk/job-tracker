package com.example.job_tracker.jobapplication.history;

import com.example.job_tracker.common.exceptions.ConflictException;
import com.example.job_tracker.jobapplication.model.RecruitmentStatus;

public class InvalidStatusChangeException extends ConflictException {
    
    public InvalidStatusChangeException(RecruitmentStatus from, RecruitmentStatus to) {
        super("Cannot change status from " + from + " to " + to + ": " + from + " is a final status");
    }
}
