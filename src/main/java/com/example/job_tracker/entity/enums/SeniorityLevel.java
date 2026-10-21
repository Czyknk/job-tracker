package com.example.job_tracker.entity.enums;

public enum SeniorityLevel {
    
    INTERN("Intern"),
    JUNIOR("Junior"),
    MID("Mid Level"),
    SENIOR("Senior");
    
    private final String displayName;

    SeniorityLevel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
