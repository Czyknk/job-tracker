package com.example.job_tracker.entity.enums;

public enum WorkMode {
    
    REMOTE("Zdalnie"),
    HYBRID("Hybrydowo"),
    ONSITE("Stacjonarnie");
    
    private final String displayName;
    
    WorkMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
