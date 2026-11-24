package com.example.job_tracker.jobapplication.history;

import java.util.List;
import java.util.Optional;

import com.example.job_tracker.jobapplication.model.RecruitmentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {

    // Retrieves all status history entries for a given application, ordered by the time they were changed (ascending) and then by their ID (ascending).
    @EntityGraph(attributePaths = {"contacts", "contacts.company"})
    List<StatusHistory> findByApplicationIdOrderByChangedAtAscIdAsc(Long applicationId);

    Optional<StatusHistory> findByIdAndApplicationId(Long id, Long applicationId);

    Optional<StatusHistory> findFirstByApplicationIdAndStatusNotOrderByChangedAtDescIdDesc(
            Long applicationId, RecruitmentStatus status);
}
