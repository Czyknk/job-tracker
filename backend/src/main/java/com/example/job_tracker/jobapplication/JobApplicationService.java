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
import com.example.job_tracker.jobapplication.model.ArchiveScope;
import com.example.job_tracker.jobapplication.model.RecruitmentStatus;
import jakarta.persistence.EntityManager;
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
    private final EntityManager entityManager;
    
    // Statuses in which an application waits for the employer (open, already applied)
    private static final Set<RecruitmentStatus> STALE_CANDIDATES = RecruitmentStatus.OPEN.stream()
            .filter(status -> status != RecruitmentStatus.FOUND)
            .collect(Collectors.toUnmodifiableSet());

    private static final Set<String> SORTABLE =
            Set.of("dateFound", "dateApplied", "deadline", "position", "currentStatus", "createdAt",
                    "lastStatusChangedAt");

    /**
     * Saves the application and immediately creates the first history entry, so that the history
     * always starts with the current status.
     */
    @Transactional
    public JobApplication create(JobApplication application) {
        LocalDate dateFound = application.getDateFound();
        LocalDate dateApplied = application.getDateApplied();
        if (dateApplied != null && dateApplied.isBefore(dateFound)) {
            throw new BadRequestException("dateApplied cannot be before dateFound");
        }
        JobApplication saved = applicationRepository.save(application);
        statusHistoryRepository.save(
                new StatusHistory(saved, RecruitmentStatus.FOUND,
                        instantAtStartOfDay(dateFound), null));
        if (dateApplied != null) {
            statusHistoryRepository.save(
                    new StatusHistory(saved, RecruitmentStatus.APPLIED,
                            instantAtStartOfDay(dateApplied), null));
            saved.setCurrentStatus(RecruitmentStatus.APPLIED);
        } else {
            saved.setCurrentStatus(RecruitmentStatus.FOUND);
        }
        return reloadDerived(saved);
    }

    private Instant instantAtStartOfDay(LocalDate date) {
        return date.atStartOfDay(clock.getZone()).toInstant();
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
        application.setDateFound(request.dateFound() != null ? request.dateFound() : LocalDate.now(clock));
        if (request.dateApplied() != null
                && request.dateApplied().isBefore(application.getDateFound())) {
            throw new BadRequestException("dateApplied cannot be before dateFound");
        }
        application.setDateApplied(request.dateApplied());
        application.setNextStepAt(request.nextStepAt());
        application.setNextStepNote(blankToNull(request.nextStepNote()));
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
        if (request.dateApplied() != null && request.dateApplied().isBefore(request.dateFound())) {
            throw new BadRequestException("dateApplied cannot be before dateFound");
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
        application.setNextStepAt(request.nextStepAt());
        application.setNextStepNote(blankToNull(request.nextStepNote()));
        synchronizeHistoryDates(application);
        return reloadDerived(application);
    }

    /**
     * Dates stored on JobApplication are projections of the history:
     * dateFound comes from FOUND and dateApplied from APPLIED.
     */
    private void synchronizeHistoryDates(JobApplication application) {
        List<StatusHistory> entries =
                statusHistoryRepository.findByApplicationIdOrderByChangedAtAscIdAsc(application.getId());
        if (entries == null) {
            entries = List.of();
        }

        entries.stream()
                .filter(entry -> entry.getStatus() == RecruitmentStatus.FOUND)
                .min(Comparator.comparing(StatusHistory::getChangedAt).thenComparing(StatusHistory::getId))
                .ifPresent(entry -> entry.correct(
                        RecruitmentStatus.FOUND,
                        instantAtStartOfDay(application.getDateFound()),
                        entry.getNote()));

        List<StatusHistory> applied = entries.stream()
                .filter(entry -> entry.getStatus() == RecruitmentStatus.APPLIED)
                .sorted(Comparator.comparing(StatusHistory::getChangedAt).thenComparing(StatusHistory::getId))
                .toList();

        if (application.getDateApplied() == null) {
            statusHistoryRepository.deleteAll(applied);
        } else if (applied.isEmpty()) {
            statusHistoryRepository.save(new StatusHistory(
                    application,
                    RecruitmentStatus.APPLIED,
                    instantAtStartOfDay(application.getDateApplied()),
                    null));
        } else {
            StatusHistory firstApplied = applied.getFirst();
            firstApplied.correct(
                    RecruitmentStatus.APPLIED,
                    instantAtStartOfDay(application.getDateApplied()),
                    firstApplied.getNote());
        }

        entries = statusHistoryRepository.findByApplicationIdOrderByChangedAtAscIdAsc(application.getId());
        if (entries == null) {
            entries = List.of();
        }
        if (!entries.isEmpty()) {
            StatusHistory latest = entries.stream()
                    .max(Comparator
                            .comparing((StatusHistory entry) -> entry.getStatus() == RecruitmentStatus.FOUND ? 0 : 1)
                            .thenComparing(StatusHistory::getChangedAt)
                            .thenComparing(StatusHistory::getId))
                    .orElseThrow();
            application.setCurrentStatus(latest.getStatus());
        }
    }

    // Writes pending changes and re-reads the entity, so derived values (lastStatusChangedAt) and the
    // version in the response reflect the history that was just written.
    private JobApplication reloadDerived(JobApplication application) {
        applicationRepository.flush();
        entityManager.refresh(application);
        return application;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    @Transactional
    public void delete(Long id) {
        applicationRepository.delete(get(id));
    }

    // Without an explicit archive or active filter the list hides archived applications, as before
    private static ArchiveScope archiveScope(ApplicationFilter filter) {
        if (filter.archived() != null) {
            return filter.archived();
        }
        return filter.active() == null ? ArchiveScope.ACTIVE : ArchiveScope.ALL;
    }

    @Transactional(readOnly = true)
    public Page<JobApplication> search(ApplicationFilter filter, Pageable pageable) {
        pageable.getSort().forEach(order -> {
            if (!SORTABLE.contains(order.getProperty())) {
                throw new BadRequestException("Unsupported sort property: " + order.getProperty());
            }
        });
        LocalDate today = LocalDate.now(clock);
        Specification<JobApplication> spec = (root, _, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.status() != null && !filter.status().isEmpty())
                predicates.add(root.get("currentStatus").in(filter.status()));
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
            switch (archiveScope(filter)) {
                case ACTIVE -> predicates.add(cb.isFalse(root.get("archived")));
                case ARCHIVED -> predicates.add(cb.isTrue(root.get("archived")));
                case ALL -> {
                }
            }
            if (Boolean.TRUE.equals(filter.active())) {
                predicates.add(cb.isFalse(root.get("archived")));
                predicates.add(cb.not(root.get("currentStatus").in(RecruitmentStatus.CLOSED)));
            } else if (Boolean.FALSE.equals(filter.active())) {
                predicates.add(cb.or(
                        cb.isTrue(root.get("archived")),
                        root.get("currentStatus").in(RecruitmentStatus.CLOSED)));
            }
            if (filter.staleDays() != null) {
                // Stale when the last change happened on or before (today - N), by calendar day
                Instant cutoff = instantAtStartOfDay(today.minusDays(filter.staleDays() - 1L));
                predicates.add(cb.isFalse(root.get("archived")));
                predicates.add(root.get("currentStatus").in(STALE_CANDIDATES));
                predicates.add(cb.lessThan(root.get("lastStatusChangedAt"), cutoff));
            }
            if (filter.deadlineWithinDays() != null) {
                predicates.add(cb.between(root.get("deadline"), today, today.plusDays(filter.deadlineWithinDays())));
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
