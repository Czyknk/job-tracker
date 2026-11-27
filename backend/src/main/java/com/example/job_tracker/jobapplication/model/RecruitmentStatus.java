package com.example.job_tracker.jobapplication.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

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

    /**
     * Statuses that end the process for lists and the dashboard: the final statuses plus GHOSTED.
     * GHOSTED is not final in the history (see {@link #isTerminal()}), so a late reply can still move it on.
     */
    public static final Set<RecruitmentStatus> CLOSED =
            Collections.unmodifiableSet(EnumSet.of(ACCEPTED, REJECTED, WITHDRAWN, GHOSTED));

    /**
     * Open statuses, iterated in pipeline order.
     */
    public static final Set<RecruitmentStatus> OPEN =
            Collections.unmodifiableSet(EnumSet.complementOf(EnumSet.copyOf(CLOSED)));

    /**
     * Final statuses: nothing may follow them in the history.
     */
    public boolean isTerminal() {
        return this == ACCEPTED || this == REJECTED || this == WITHDRAWN;
    }

    public boolean isClosed() {
        return CLOSED.contains(this);
    }
}
