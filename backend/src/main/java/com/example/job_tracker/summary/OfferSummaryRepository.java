package com.example.job_tracker.summary;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfferSummaryRepository extends JpaRepository<OfferSummary, Long> {

    List<OfferSummary> findByApplicationIdOrderByGeneratedAtDescIdDesc(Long applicationId);

    Optional<OfferSummary> findFirstByApplicationIdOrderByGeneratedAtDescIdDesc(Long applicationId);
}
