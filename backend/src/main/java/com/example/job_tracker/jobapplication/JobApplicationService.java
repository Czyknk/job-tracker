package com.example.job_tracker.jobapplication;

import com.example.job_tracker.common.exceptions.BadRequestException;
import com.example.job_tracker.common.exceptions.ConflictException;
import com.example.job_tracker.common.exceptions.ResourceNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import com.example.job_tracker.company.Company;
import com.example.job_tracker.company.CompanyRepository;
import com.example.job_tracker.contact.Contact;
import com.example.job_tracker.contact.ContactRepository;
import com.example.job_tracker.jobapplication.dto.ApplicationFilter;
import com.example.job_tracker.jobapplication.dto.CreateJobApplicationRequest;
import com.example.job_tracker.jobapplication.dto.UpdateJobApplicationRequest;
import com.example.job_tracker.jobapplication.history.InvalidStatusChangeException;
import com.example.job_tracker.jobapplication.history.StatusHistory;
import com.example.job_tracker.jobapplication.history.StatusHistoryRepository;
import com.example.job_tracker.jobapplication.model.RecruitmentStatus;
import jakarta.persistence.criteria.Join;
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
     * Saves the application and immediately creates the first history entry, so that the history
     * always starts with the current status.
     */
    @Transactional
    public JobApplication create(JobApplication application) {
        JobApplication saved = applicationRepository.save(application);
        statusHistoryRepository.save(
                new StatusHistory(saved, saved.getCurrentStatus(), clock.instant(), null));
        return saved;
    }

    /**
     * Changes the status of the job application: adds an entry to the history and updates the currentStatus in a single transaction.
     * changedAt is optional (defaults to "now"); currentStatus always corresponds to the most recently added entry.
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
        // Resolve the contacts before making any changes
        Set<Contact> contacts = resolveContacts(contactIds);

        Instant effectiveChangedAt = changedAt != null ? changedAt : clock.instant();
        // FOUND is the starting point, so only real stages decide whether the new entry is the latest one
        Optional<StatusHistory> latestStage = statusHistoryRepository
                .findFirstByApplicationIdAndStatusNotOrderByChangedAtDescIdDesc(applicationId, RecruitmentStatus.FOUND);
        boolean becomesLatest = latestStage.isEmpty()
                || !effectiveChangedAt.isBefore(latestStage.get().getChangedAt());
        if (!becomesLatest && newStatus.isTerminal()) {
            throw new ConflictException("A final status must be the last history entry");
        }

        StatusHistory entry = new StatusHistory(application, newStatus, effectiveChangedAt, note);
        contacts.forEach(entry::addContact);
        StatusHistory saved = statusHistoryRepository.save(entry);

        if (becomesLatest) {
            application.setCurrentStatus(newStatus);
        }
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
        return statusHistoryRepository.findByApplicationIdOrderByChangedAtAscIdAsc(applicationId).stream()
                .sorted(Comparator.comparing((StatusHistory e) -> e.getStatus() == RecruitmentStatus.FOUND ? 0 : 1)
                        .thenComparing(StatusHistory::getChangedAt)
                        .thenComparing(StatusHistory::getId))
                .toList();
    }

    @Transactional
    public JobApplication updateNotes(Long id, String notes) {
        JobApplication application = get(id);
        application.setNotes(blankToNull(notes));
        return application;
    }

    @Transactional
    public JobApplication setArchived(Long id, boolean archived) {
        JobApplication application = get(id);
        application.setArchived(archived);
        return application;
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
        application.setNotes(blankToNull(request.notes()));
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

    @Transactional
    public JobApplication update(Long id, UpdateJobApplicationRequest request) {
        JobApplication application = get(id);
        if (!application.getVersion().equals(request.version())) {
            throw new ConflictException("The application was modified in the meantime; reload it and try again");
        }

        if (!application.getCompany().getId().equals(request.companyId())) {
            application.setCompany(companyRepository.findById(request.companyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Company", request.companyId())));
        }
        application.setPosition(request.position());
        application.setOfferUrl(request.offerUrl());
        application.setSource(request.source());
        application.setLocation(request.location());
        application.setWorkMode(request.workMode());
        application.setEmploymentType(request.employmentType());
        application.setSeniorityLevel(request.seniorityLevel());
        application.setSalary(request.salary() == null ? null : request.salary().toEntity());
        application.setRawDescription(request.rawDescription());
        application.setDeadline(request.deadline());
        application.setNotes(blankToNull(request.notes()));
        application.setDateFound(request.dateFound());
        application.setDateApplied(request.dateApplied());
        applicationRepository.flush();
        return application;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    @Transactional
    public void delete(Long id) {
        applicationRepository.delete(get(id));
    }

    @Transactional(readOnly = true)
    public Page<JobApplication> search(ApplicationFilter filter, Pageable pageable) {
        pageable.getSort().forEach(order -> {
            if (!SORTABLE.contains(order.getProperty())) {
                throw new BadRequestException("Unsupported sort property: " + order.getProperty());
            }
        });
        Specification<JobApplication> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.status() != null) predicates.add(cb.equal(root.get("currentStatus"), filter.status()));
            if (filter.companyId() != null) predicates.add(cb.equal(root.get("company").get("id"), filter.companyId()));
            if (filter.source() != null) predicates.add(cb.equal(root.get("source"), filter.source()));
            if (filter.workMode() != null) predicates.add(cb.equal(root.get("workMode"), filter.workMode()));
            if (filter.employmentType() != null)
                predicates.add(cb.equal(root.get("employmentType"), filter.employmentType()));
            if (filter.seniorityLevel() != null)
                predicates.add(cb.equal(root.get("seniorityLevel"), filter.seniorityLevel()));
            if (filter.foundFrom() != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("dateFound"), filter.foundFrom()));
            if (filter.foundTo() != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("dateFound"), filter.foundTo()));
            if (filter.deadlineBefore() != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("deadline"), filter.deadlineBefore()));
            switch (filter.archived()) {
                case ACTIVE -> predicates.add(cb.isFalse(root.get("archived")));
                case ARCHIVED -> predicates.add(cb.isTrue(root.get("archived")));
                case ALL -> {
                }
            }
            String[] terms = filter.q() == null ? new String[0] : filter.q().trim().toLowerCase().split("\\s+");
            if (terms.length > 0 && !terms[0].isEmpty()) {
                Join<JobApplication, Company> company = root.join("company");
                for (String term : terms) {
                    String pattern = "%" + term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("position")), pattern, '\\'),
                            cb.like(cb.lower(company.get("name")), pattern, '\\'),
                            cb.like(cb.lower(root.get("location")), pattern, '\\'),
                            cb.like(cb.lower(root.get("rawDescription")), pattern, '\\'),
                            cb.like(cb.lower(root.get("notes")), pattern, '\\')));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return applicationRepository.findAll(spec, pageable);
    }
}
