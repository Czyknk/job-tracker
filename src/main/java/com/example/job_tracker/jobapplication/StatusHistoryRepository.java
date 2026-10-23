package com.example.job_tracker.jobapplication;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {

    // Drugie kryterium (id) daje deterministyczną kolejność dla wpisów z identycznym changedAt.
    List<StatusHistory> findByApplicationIdOrderByChangedAtAscIdAsc(Long applicationId);
}
