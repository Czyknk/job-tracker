package com.example.job_tracker.demo;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.job_tracker.company.Company;
import com.example.job_tracker.company.CompanyService;
import com.example.job_tracker.company.dto.CompanyRequest;
import com.example.job_tracker.contact.Contact;
import com.example.job_tracker.contact.ContactService;
import com.example.job_tracker.contact.dto.ContactRequest;
import com.example.job_tracker.document.Document;
import com.example.job_tracker.document.DocumentService;
import com.example.job_tracker.document.dto.DocumentRequest;
import com.example.job_tracker.document.dto.UpdateDocumentRequest;
import com.example.job_tracker.document.enums.DocumentType;
import com.example.job_tracker.jobapplication.JobApplication;
import com.example.job_tracker.jobapplication.JobApplicationService;
import com.example.job_tracker.jobapplication.documents.ApplicationDocumentService;
import com.example.job_tracker.jobapplication.dto.CreateJobApplicationRequest;
import com.example.job_tracker.jobapplication.dto.SalaryRangeDto;
import com.example.job_tracker.jobapplication.model.*;
import com.example.job_tracker.summary.OfferSummary;
import com.example.job_tracker.summary.OfferSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Fills an empty database with fictional sample data. Everything goes through the regular services,
 * so the data respects the same invariants as data created through the API (initial history entry,
 * dateApplied, document rules). All dates are relative to "now", so the demo always looks fresh.
 * Company names, people and URLs are fictional (example.com domains).
 */
@Component
@ConditionalOnProperty(prefix = "app.demo", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class DemoDataSeeder {

    private static final String SAMPLE_MODEL = "demo-sample";

    private final CompanyService companyService;
    private final ContactService contactService;
    private final DocumentService documentService;
    private final JobApplicationService applicationService;
    private final ApplicationDocumentService applicationDocumentService;
    private final OfferSummaryRepository summaryRepository;
    private final Clock clock;

    public void seed() {
        // Companies and people
        Company northwind = company("Northwind Software", "Software house", "https://northwind.example.com");
        Company contoso = company("Contoso Fintech", "Fintech", "https://contoso.example.com");
        Company globex = company("Globex Cloud", "Cloud services", "https://globex.example.com");
        Company initech = company("Initech Logistics", "Logistics software", "https://initech.example.com");
        Company umbrella = company("Umbrella Health", "HealthTech", "https://umbrella.example.com");

        Contact anna = contact(northwind, "Anna", "Nowak", "Tech Recruiter", "anna.nowak@northwind.example.com");
        Contact piotr = contact(northwind, "Piotr", "Zielinski", "Senior Java Engineer", null);
        Contact marta = contact(contoso, "Marta", "Kowalska", "HR Business Partner", "marta.kowalska@contoso.example.com");
        contact(globex, "Tomasz", "Lewandowski", "Engineering Manager", "tomasz.lewandowski@globex.example.com");

        // Documents: v1 is attached to an application first and archived afterwards (archived documents
        // cannot be attached)
        Document cvV1 = document("CV_Java_Backend_v1", DocumentType.CV, 60);
        Document cvV2 = document("CV_Java_Backend_v2", DocumentType.CV, 20);
        Document letter = document("Cover_Letter_Northwind", DocumentType.COVER_LETTER, 22);

        // 1. Offer received
        JobApplication northwindJob = application(northwind, "Junior Java Developer", JobSource.PRACUJ_PL,
                "Warsaw", WorkMode.HYBRID, EmploymentType.UOP, SeniorityLevel.JUNIOR,
                salary(8000, 11000, SalaryPeriod.MONTH, SalaryBasis.GROSS),
                "We are looking for a Junior Java Developer to join our product team. You will build REST APIs "
                        + "with Spring Boot and PostgreSQL, write automated tests and take part in code reviews.",
                null, 25);
        status(northwindJob, RecruitmentStatus.APPLIED, 21, "Applied through the company website");
        status(northwindJob, RecruitmentStatus.HR_SCREEN, 14, "30-minute call about motivation and salary", anna);
        status(northwindJob, RecruitmentStatus.TECH_INTERVIEW, 7, "Interview with the team: Java, SQL, design basics", anna, piotr);
        status(northwindJob, RecruitmentStatus.OFFER_RECEIVED, 2, "Verbal offer received, written offer expected this week");
        attach(northwindJob, cvV2);
        attach(northwindJob, letter);
        summary(northwindJob, "Rola: Junior Java Developer w zespole produktowym, tworzenie REST API.\n"
                + "Wymagania:\n- Java i Spring Boot\n- PostgreSQL\n- testy automatyczne\n- code review\n"
                + "Technologie: Java, Spring Boot, PostgreSQL\n"
                + "Warunki: umowa o pracę, 8000-11000 PLN brutto, Warszawa (hybrydowo).");

        // 2. Rejected after a take-home task
        JobApplication contosoJob = application(contoso, "Java Backend Developer", JobSource.LINKEDIN,
                "Remote", WorkMode.REMOTE, EmploymentType.B2B, SeniorityLevel.JUNIOR,
                salary(110, 140, SalaryPeriod.HOUR, SalaryBasis.NET),
                "Join our payments team and work on services that process card transactions. Experience with "
                        + "Spring Boot, relational databases and message queues is a plus.",
                null, 20);
        status(contosoJob, RecruitmentStatus.APPLIED, 18, null);
        status(contosoJob, RecruitmentStatus.HR_SCREEN, 11, "Intro call", marta);
        status(contosoJob, RecruitmentStatus.TECH_TASK, 6, "Take-home task: a small REST API with Spring Boot");
        status(contosoJob, RecruitmentStatus.REJECTED, 1, "Feedback: they need more production experience", marta);
        attach(contosoJob, cvV2);

        // 3. Waiting for an answer, with an AI-style summary
        JobApplication globexJob = application(globex, "Junior Software Engineer (Java)", JobSource.THE_PROTOCOL,
                "Krakow", WorkMode.HYBRID, EmploymentType.UOP, SeniorityLevel.JUNIOR,
                salary(7500, 10000, SalaryPeriod.MONTH, SalaryBasis.GROSS),
                "Our cloud platform team is hiring a Junior Software Engineer. You will work with Java, Docker "
                        + "and CI/CD pipelines, and learn Kubernetes from experienced colleagues.",
                null, 10);
        status(globexJob, RecruitmentStatus.APPLIED, 9, "Sent the application through the job portal");
        attach(globexJob, cvV2);
        summary(globexJob, "Rola: Junior Software Engineer w zespole platformy chmurowej.\n"
                + "Wymagania:\n- Java\n- Docker\n- podstawy CI/CD\n"
                + "Technologie: Java, Docker, CI/CD, Kubernetes (nauka na miejscu)\n"
                + "Warunki: umowa o pracę, 7500-10000 PLN brutto, Kraków (hybrydowo).");

        // 4. Found, deadline in 10 days
        application(initech, "Backend Developer Intern", JobSource.PRACUJ_PL,
                "Wroclaw", WorkMode.ONSITE, EmploymentType.OTHER, SeniorityLevel.INTERN, null,
                "Paid internship in the backend team of a logistics software company. Mentoring included.",
                10, 3);

        // 5. Ghosted
        JobApplication umbrellaJob = application(umbrella, "Java Developer", JobSource.LINKEDIN,
                "Remote", WorkMode.REMOTE, EmploymentType.UOP, SeniorityLevel.JUNIOR, null,
                "Develop backend services for a telemedicine platform using Java and Spring Boot.",
                null, 35);
        status(umbrellaJob, RecruitmentStatus.APPLIED, 30, null);
        status(umbrellaJob, RecruitmentStatus.GHOSTED, 5, "No answer after two follow-up e-mails");
        attach(umbrellaJob, cvV1);

        // 6. Found, deadline in 4 days
        application(globex, "Junior Kotlin/Java Developer", JobSource.OTHER,
                "Krakow", WorkMode.HYBRID, EmploymentType.UOP, SeniorityLevel.JUNIOR, null,
                "Backend developer for the billing team (Kotlin and Java on the JVM).",
                4, 1);

        // Archive the old CV now that it is attached to a past application
        documentService.update(cvV1.getId(), new UpdateDocumentRequest(cvV1.getName(), DocumentType.CV, true));
    }

    // ------------------------------------------------------------------ helpers

    private Company company(String name, String industry, String website) {
        return companyService.create(new CompanyRequest(name, industry, website, null));
    }

    private Contact contact(Company company, String firstName, String lastName, String position, String email) {
        return contactService.create(
                new ContactRequest(company.getId(), firstName, lastName, position, email, null, null, null));
    }

    private Document document(String name, DocumentType type, int uploadedDaysAgo) {
        return documentService.create(
                new DocumentRequest(name, type, LocalDate.now(clock).minusDays(uploadedDaysAgo)));
    }

    private static SalaryRangeDto salary(int min, int max, SalaryPeriod period, SalaryBasis basis) {
        return new SalaryRangeDto(BigDecimal.valueOf(min), BigDecimal.valueOf(max), "PLN", period, basis);
    }

    private JobApplication application(Company company, String position, JobSource source, String location,
                                       WorkMode workMode, EmploymentType employmentType,
                                       SeniorityLevel seniority, SalaryRangeDto salary, String description,
                                       Integer deadlineInDays, int foundDaysAgo) {
        LocalDate today = LocalDate.now(clock);
        return applicationService.create(new CreateJobApplicationRequest(
                company.getId(), position,
                "https://careers." + company.getName().toLowerCase().replace(' ', '-') + ".example/jobs",
                source, location, workMode, employmentType, seniority, salary, description,
                deadlineInDays == null ? null : today.plusDays(deadlineInDays),
                today.minusDays(foundDaysAgo), null));
    }

    private void status(JobApplication application, RecruitmentStatus status, int daysAgo, String note,
                        Contact... contacts) {
        Instant changedAt = clock.instant().minus(Duration.ofDays(daysAgo));
        Set<Long> contactIds = Arrays.stream(contacts).map(Contact::getId).collect(Collectors.toSet());
        applicationService.changeStatus(application.getId(), status, changedAt, note, contactIds);
    }

    private void attach(JobApplication application, Document document) {
        applicationDocumentService.attach(application.getId(), document.getId());
    }

    private void summary(JobApplication application, String text) {
        summaryRepository.save(new OfferSummary(application, text, clock.instant(), SAMPLE_MODEL));
    }
}
