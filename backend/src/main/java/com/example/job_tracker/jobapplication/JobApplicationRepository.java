package com.example.job_tracker.jobapplication;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.example.job_tracker.jobapplication.model.RecruitmentStatus;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long>, JpaSpecificationExecutor<JobApplication> {

    @EntityGraph(attributePaths = "company")
    Optional<JobApplication> findWithCompanyById(Long id);

    @Override
    @EntityGraph(attributePaths = "company")
    Page<JobApplication> findAll(Specification<JobApplication> spec, Pageable pageable);

    interface StatusCount {
        RecruitmentStatus getStatus();

        long getTotal();
    }

    @Query("""
            select a.currentStatus as status, count(a) as total
            from JobApplication a
            where a.archived = false and a.currentStatus in :statuses
            group by a.currentStatus
            """)
    List<StatusCount> countNotArchivedByStatus(@Param("statuses") Collection<RecruitmentStatus> statuses);

    // Not archived applications in the given statuses with a next step at or after the given instant, soonest first
    @EntityGraph(attributePaths = "company")
    @Query("""
            select a from JobApplication a
            where a.archived = false and a.currentStatus in :statuses and a.nextStepAt >= :from
            order by a.nextStepAt asc, a.id asc
            """)
    List<JobApplication> findUpcoming(@Param("statuses") Collection<RecruitmentStatus> statuses,
                                      @Param("from") Instant from, Limit limit);
}
