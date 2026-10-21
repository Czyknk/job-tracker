package com.example.job_tracker.entity.enums;

public enum ApplicationSource {
    
    PRACUJ_PL("Pracuj.pl"),
    LINKEDIN("LinkedIn"),
    THE_PROTOCOL("The Protocol"),
    JUST_JOIN("Just Join"),
    OTHER("Other");
    
    private final String displayName;
    
    ApplicationSource(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

}