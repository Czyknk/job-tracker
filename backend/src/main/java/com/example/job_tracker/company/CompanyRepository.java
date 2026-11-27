package com.example.job_tracker.company;

import com.example.job_tracker.company.dto.CompanyOption;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    
    Optional<Company> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Query("select count(a) from JobApplication a where a.company.id = :companyId")
    long countApplications(@Param("companyId") Long companyId);

    // Names matching the LIKE pattern; names matching the prefix pattern come first. Both use '!' as escape char.
    @Query("""
            select new com.example.job_tracker.company.dto.CompanyOption(c.id, c.name)
            from Company c
            where lower(c.name) like :pattern escape '!'
            order by case when lower(c.name) like :prefix escape '!' then 0 else 1 end, lower(c.name), c.id
            """)
    List<CompanyOption> lookup(@Param("pattern") String pattern, @Param("prefix") String prefix, Limit limit);
}
