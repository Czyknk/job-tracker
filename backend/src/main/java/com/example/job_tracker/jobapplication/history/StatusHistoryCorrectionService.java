package com.example.job_tracker.jobapplication.history;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import com.example.job_tracker.common.exceptions.ConflictException;
import com.example.job_tracker.common.exceptions.ResourceNotFoundException;
import com.example.job_tracker.jobapplication.JobApplication;
import com.example.job_tracker.jobapplication.JobApplicationRepository;
import com.example.job_tracker.jobapplication.model.RecruitmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fixes mistakes in the status history: editing or deleting an entry. The current status of the
 * application is always the status of the latest real stage (entries other than FOUND, by time; ties: the newest entry).
 * Every change is validated on an in-memory copy of the timeline first, so a rejected correction
 * never leaves partial changes behind.
 */
@Service
@RequiredArgsConstructor
public class StatusHistoryCorrectionService {

    // Oldest first; the last element is the "current" entry. FOUND is the starting point of every
    // application, so it always comes before the real stages, whatever its timestamp is.
    private static final Comparator<Point> TIMELINE_ORDER =
            Comparator.comparing((Point p) -> p.status() == RecruitmentStatus.FOUND ? 0 : 1)
                    .thenComparing(Point::changedAt)
                    .thenComparing(Point::id);

    private final JobApplicationRepository applicationRepository;
    private final StatusHistoryRepository historyRepository;
    private final Clock clock;

    private record Point(Long id, RecruitmentStatus status, Instant changedAt) {

        static Point of(StatusHistory entry) {
            return new Point(entry.getId(), entry.getStatus(), entry.getChangedAt());
        }
    }

    @Transactional
    public StatusHistory update(Long applicationId, Long entryId, RecruitmentStatus status,
                                Instant changedAt, String note) {
        JobApplication application = findApplication(applicationId);
        List<StatusHistory> entries = historyRepository.findByApplicationIdOrderByChangedAtAscIdAsc(applicationId);
        StatusHistory entry = find(entries, entryId);

        List<Point> proposed = entries.stream()
                .map(e -> e.getId().equals(entryId) ? new Point(entryId, status, changedAt) : Point.of(e))
                .sorted(TIMELINE_ORDER)
                .toList();
        requireFinalStatusLast(proposed);
        requireFoundStatusPresent(proposed);
        requireDateOrder(proposed);

        entry.correct(status, changedAt, note);
        application.setCurrentStatus(proposed.getLast().status());
        synchronizeDerivedDates(application, proposed);
        return entry;
    }

    @Transactional
    public void delete(Long applicationId, Long entryId) {
        JobApplication application = findApplication(applicationId);
        List<StatusHistory> entries = historyRepository.findByApplicationIdOrderByChangedAtAscIdAsc(applicationId);
        StatusHistory entry = find(entries, entryId);

        if (entries.size() == 1) {
            throw new ConflictException("An application must keep at least one history entry");
        }

        List<Point> remaining = entries.stream()
                .filter(e -> !e.getId().equals(entryId))
                .map(Point::of)
                .sorted(TIMELINE_ORDER)
                .toList();
        requireFoundStatusPresent(remaining);
        requireDateOrder(remaining);

        application.setCurrentStatus(remaining.getLast().status());
        synchronizeDerivedDates(application, remaining);
        historyRepository.delete(entry);
    }

    private void synchronizeDerivedDates(JobApplication application, List<Point> timeline) {
        timeline.stream()
                .filter(point -> point.status() == RecruitmentStatus.FOUND)
                .findFirst()
                .ifPresent(point -> application.setDateFound(
                        LocalDate.ofInstant(point.changedAt(), clock.getZone())));

        timeline.stream()
                .filter(point -> point.status() == RecruitmentStatus.APPLIED)
                .findFirst()
                .ifPresentOrElse(
                        point -> application.setDateApplied(
                                LocalDate.ofInstant(point.changedAt(), clock.getZone())),
                        () -> application.setDateApplied(null));
    }

    // A final status (ACCEPTED, REJECTED, WITHDRAWN) ends the process, so nothing may come after it.
    private static void requireFinalStatusLast(List<Point> timeline) {
        for (int i = 0; i < timeline.size() - 1; i++) {
            if (timeline.get(i).status().isTerminal()) {
                throw new ConflictException("A final status must be the last history entry");
            }
        }
    }

    private static void requireFoundStatusPresent(List<Point> timeline) {
        if (timeline.stream().noneMatch(point -> point.status() == RecruitmentStatus.FOUND)) {
            throw new ConflictException("An application must keep a FOUND history entry");
        }
    }

    private void requireDateOrder(List<Point> timeline) {
        LocalDate dateFound = timeline.stream()
                .filter(point -> point.status() == RecruitmentStatus.FOUND)
                .findFirst()
                .map(point -> LocalDate.ofInstant(point.changedAt(), clock.getZone()))
                .orElse(null);
        LocalDate dateApplied = timeline.stream()
                .filter(point -> point.status() == RecruitmentStatus.APPLIED)
                .findFirst()
                .map(point -> LocalDate.ofInstant(point.changedAt(), clock.getZone()))
                .orElse(null);
        if (dateFound != null && dateApplied != null && dateApplied.isBefore(dateFound)) {
            throw new ConflictException("dateApplied cannot be before dateFound");
        }
    }

    private JobApplication findApplication(Long applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("JobApplication", applicationId));
    }

    private static StatusHistory find(List<StatusHistory> entries, Long entryId) {
        return entries.stream()
                .filter(e -> e.getId().equals(entryId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("StatusHistory entry", entryId));
    }
}
