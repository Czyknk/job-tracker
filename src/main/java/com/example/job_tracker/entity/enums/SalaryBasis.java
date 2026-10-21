package com.example.job_tracker.entity.enums;

public enum SalaryBasis {
    
    GROSS("Brutto"),
    NET("Netto");
    
    private final String displayName;

    SalaryBasis(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
