package com.example.job_tracker.dashboard;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.example.job_tracker.dashboard.dto.AttentionItem;
import com.example.job_tracker.dashboard.dto.AttentionReason;
import com.example.job_tracker.dashboard.dto.DashboardResponse;
import com.example.job_tracker.dashboard.dto.RecentActivityItem;
import com.example.job_tracker.jobapplication.JobApplicationRepository;
import com.example.job_tracker.jobapplication.JobApplicationService;
import com.example.job_tracker.jobapplication.dto.ApplicationFilter;
import com.example.job_tracker.jobapplication.dto.JobApplicationListItem;
import com.example.job_tracker.jobapplication.history.StatusHistoryRepository;
import com.example.job_tracker.jobapplication.model.RecruitmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the "Today" view. The attention lists reuse the application list search, so they follow exactly the
 * same rules as the list filters (active, staleDays, deadlineWithinDays).
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final JobApplicationService applicationService;
    private final JobApplicationRepository applicationRepository;
    private final StatusHistoryRepository historyRepository;
    private final DashboardProperties properties;
    private final Clock clock;

    @Transactional(readOnly = true)
    public DashboardResponse get() {
        return new DashboardResponse(activeByStatus(), needsAttention(), upcoming(), recentActivity());
    }

    private Map<RecruitmentStatus, Long> activeByStatus() {
        Map<RecruitmentStatus, Long> counts = new LinkedHashMap<>();
        RecruitmentStatus.OPEN.forEach(status -> counts.put(status, 0L));
        applicationRepository.countNotArchivedByStatus(RecruitmentStatus.OPEN)
                .forEach(count -> counts.put(count.getStatus(), count.getTotal()));
        return counts;
    }

    private List<AttentionItem> needsAttention() {
        List<AttentionItem> items = new ArrayList<>();
        add(items, AttentionReason.DEADLINE_SOON,
                activeFilter(null, null, properties.deadlineDays()),
                Sort.by("deadline", "dateFound"));
        add(items, AttentionReason.NO_RESPONSE,
                activeFilter(null, properties.staleDays(), null),
                Sort.by("lastStatusChangedAt", "dateFound"));
        add(items, AttentionReason.NOT_APPLIED_YET,
                activeFilter(Set.of(RecruitmentStatus.FOUND), null, null),
                Sort.by("dateFound", "createdAt"));
        return items;
    }

    private void add(List<AttentionItem> items, AttentionReason reason, ApplicationFilter filter, Sort sort) {
        applicationService.search(filter, PageRequest.of(0, properties.limit(), sort))
                .forEach(application -> items.add(new AttentionItem(reason, JobApplicationListItem.from(application))));
    }

    private List<JobApplicationListItem> upcoming() {
        // From the start of today, so a step earlier today stays visible for the rest of the day
        Instant startOfToday = LocalDate.now(clock).atStartOfDay(clock.getZone()).toInstant();
        return applicationRepository
                .findUpcoming(RecruitmentStatus.OPEN, startOfToday, Limit.of(properties.limit()))
                .stream().map(JobApplicationListItem::from).toList();
    }

    private List<RecentActivityItem> recentActivity() {
        return historyRepository.findRecentActivity(Limit.of(properties.limit())).stream()
                .map(RecentActivityItem::from).toList();
    }

    private static ApplicationFilter activeFilter(Set<RecruitmentStatus> status, Integer staleDays,
                                                  Integer deadlineWithinDays) {
        return new ApplicationFilter(status, null, null, null, null, null, null, null, null, null, null,
                true, staleDays, deadlineWithinDays);
    }
}
