package com.example.job_tracker.jobapplication.history;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {

    // Drugie kryterium (id) daje deterministyczną kolejność dla wpisów z identycznym changedAt.
    @EntityGraph(attributePaths = {"contacts", "contacts.company"})
    List<StatusHistory> findByApplicationIdOrderByChangedAtAscIdAsc(Long applicationId);

    Optional<StatusHistory> findByIdAndApplicationId(Long id, Long applicationId);
}
