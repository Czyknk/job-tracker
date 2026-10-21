package com.example.job_tracker.entity.enums;

public enum EmploymentType {
    
    UOP("UoP"),
    B2B("B2B"),
    ZLECENIE("Zlecenie"),
    OTHER("Other");
    
    private final String displayName;

    EmploymentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
