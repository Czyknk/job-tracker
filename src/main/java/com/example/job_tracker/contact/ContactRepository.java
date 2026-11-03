package com.example.job_tracker.contact;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Wszystkie zapytania doładowują firmę (EntityGraph), bo mapowanie na DTO odbywa się poza transakcją.
 */
public interface ContactRepository extends JpaRepository<Contact, Long> {

    @EntityGraph(attributePaths = "company")
    Optional<Contact> findWithCompanyById(Long id);

    @EntityGraph(attributePaths = "company")
    List<Contact> findWithCompanyByIdIn(Collection<Long> ids);

    @EntityGraph(attributePaths = "company")
    List<Contact> findAllBy(Sort sort);

    @EntityGraph(attributePaths = "company")
    List<Contact> findByCompanyId(Long companyId, Sort sort);
}
