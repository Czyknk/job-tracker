package com.example.job_tracker.jobapplication;

import com.example.job_tracker.common.exceptions.BadRequestException;
import com.example.job_tracker.common.exceptions.ResourceNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.job_tracker.company.Company;
import com.example.job_tracker.company.CompanyRepository;
import com.example.job_tracker.contact.Contact;
import com.example.job_tracker.contact.ContactRepository;
import com.example.job_tracker.jobapplication.dto.CreateJobApplicationRequest;
import com.example.job_tracker.jobapplication.enums.RecruitmentStatus;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JobApplicationService {

    private final JobApplicationRepository applicationRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final CompanyRepository companyRepository;
    private final ContactRepository contactRepository;
    private final Clock clock;
    
    private static final Set<String> SORTABLE =
            Set.of("dateFound", "dateApplied", "deadline", "position", "currentStatus", "createdAt");

    /**
     * Zapisuje aplikację i od razu zakłada pierwszy wpis historii, żeby historia
     * zawsze zaczynała się od bieżącego statusu.
     */
    @Transactional
    public JobApplication create(JobApplication application) {
        JobApplication saved = applicationRepository.save(application);
        statusHistoryRepository.save(
                new StatusHistory(saved, saved.getCurrentStatus(), clock.instant(), null));
        return saved;
    }

    /**
     * Zmienia status: dopisuje wpis do historii i aktualizuje currentStatus w jednej transakcji.
     * changedAt jest opcjonalne (domyślnie "teraz"); currentStatus zawsze odpowiada ostatnio dodanemu wpisowi.
     */
    @Transactional
    public StatusHistory changeStatus(Long applicationId, RecruitmentStatus newStatus,
                                      Instant changedAt, String note) {
        return changeStatus(applicationId, newStatus, changedAt, note, Set.of());
    }

    @Transactional
    public StatusHistory changeStatus(Long applicationId, RecruitmentStatus newStatus,
                                      Instant changedAt, String note, Set<Long> contactIds) {
        JobApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("JobApplication", applicationId));

        RecruitmentStatus current = application.getCurrentStatus();
        if (current.isTerminal()) {
            throw new InvalidStatusChangeException(current, newStatus);
        }
        Set<Contact> contacts = resolveContacts(contactIds);   // przed jakąkolwiek zmianą

        Instant effectiveChangedAt = changedAt != null ? changedAt : clock.instant();
        StatusHistory entry = new StatusHistory(application, newStatus, effectiveChangedAt, note);
        contacts.forEach(entry::addContact);
        StatusHistory saved = statusHistoryRepository.save(entry);

        application.setCurrentStatus(newStatus);
        if (newStatus == RecruitmentStatus.APPLIED && application.getDateApplied() == null) {
            application.setDateApplied(LocalDate.ofInstant(effectiveChangedAt, clock.getZone()));
        }
        return saved;
    }

    private Set<Contact> resolveContacts(Set<Long> contactIds) {
        if (contactIds == null || contactIds.isEmpty()) {
            return Set.of();
        }
        List<Contact> found = contactRepository.findWithCompanyByIdIn(contactIds);
        Set<Long> foundIds = found.stream().map(Contact::getId).collect(Collectors.toSet());
        contactIds.stream().filter(id -> !foundIds.contains(id)).findFirst()
                .ifPresent(id -> { throw new ResourceNotFoundException("Contact", id); });
        return new HashSet<>(found);
    }

    @Transactional(readOnly = true)
    public List<StatusHistory> getHistory(Long applicationId) {
        if (!applicationRepository.existsById(applicationId)) {
            throw new ResourceNotFoundException("JobApplication", applicationId);
        }
        return statusHistoryRepository.findByApplicationIdOrderByChangedAtAscIdAsc(applicationId);
    }

    @Transactional
    public JobApplication create(CreateJobApplicationRequest request) {
        Company company = companyRepository.findById(request.companyId())
                .orElseThrow(() -> new ResourceNotFoundException("Company", request.companyId()));
        JobApplication application = new JobApplication(company, request.position());
        application.setOfferUrl(request.offerUrl());
        if (request.source() != null) {
            application.setSource(request.source());
        }
        application.setLocation(request.location());
        application.setWorkMode(request.workMode());
        application.setEmploymentType(request.employmentType());
        application.setSeniorityLevel(request.seniorityLevel());
        if (request.salary() != null) {
            application.setSalary(request.salary().toEntity());
        }
        application.setRawDescription(request.rawDescription());
        application.setDeadline(request.deadline());
        if (request.dateFound() != null) {
            application.setDateFound(request.dateFound());
        }
        application.setDateApplied(request.dateApplied());
        return create(application);
    }

    @Transactional(readOnly = true)
    public JobApplication get(Long id) {
        return applicationRepository.findWithCompanyById(id)
                .orElseThrow(() -> new ResourceNotFoundException("JobApplication", id));
    }

    @Transactional(readOnly = true)
    public Page<JobApplication> search(RecruitmentStatus status, Long companyId, Pageable pageable) {
        pageable.getSort().forEach(order -> {
            if (!SORTABLE.contains(order.getProperty())) {
                throw new BadRequestException("Unsupported sort property: " + order.getProperty());
            }
        });
        Specification<JobApplication> spec = (root, _, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("currentStatus"), status));
            }
            if (companyId != null) {
                predicates.add(cb.equal(root.get("company").get("id"), companyId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return applicationRepository.findAll(spec, pageable);
    }
}
