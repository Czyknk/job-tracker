package com.example.job_tracker.dashboard.dto;

/**
 * Why an active application is shown in "needs attention". The rules match the list filters.
 */
public enum AttentionReason {
    /** The deadline is between today and today + app.dashboard.deadline-days (list filter deadlineWithinDays). */
    DEADLINE_SOON,
    /** Applied, but no status change for app.dashboard.stale-days days (list filter staleDays). */
    NO_RESPONSE,
    /** Still FOUND: not applied yet. */
    NOT_APPLIED_YET
}
