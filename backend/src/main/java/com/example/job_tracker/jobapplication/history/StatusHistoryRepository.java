package com.example.job_tracker.jobapplication.history;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.example.job_tracker.jobapplication.model.RecruitmentStatus;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {

    // Retrieves all status history entries for a given application, ordered by the time they were changed (ascending) and then by their ID (ascending).
    @EntityGraph(attributePaths = {"contacts", "contacts.company"})
    List<StatusHistory> findByApplicationIdOrderByChangedAtAscIdAsc(Long applicationId);

    Optional<StatusHistory> findByIdAndApplicationId(Long id, Long applicationId);

    Optional<StatusHistory> findFirstByApplicationIdAndStatusNotOrderByChangedAtDescIdDesc(
            Long applicationId, RecruitmentStatus status);

    interface RecentActivity {
        Long getEntryId();

        Long getApplicationId();

        String getPosition();

        String getCompanyName();

        RecruitmentStatus getStatus();

        Instant getChangedAt();
    }

    // The latest history entries of all applications, newest first (index on status_history(changed_at, id))
    @Query("""
            select h.id as entryId, a.id as applicationId, a.position as position, c.name as companyName,
                   h.status as status, h.changedAt as changedAt
            from StatusHistory h join h.application a join a.company c
            order by h.changedAt desc, h.id desc
            """)
    List<RecentActivity> findRecentActivity(Limit limit);
}
