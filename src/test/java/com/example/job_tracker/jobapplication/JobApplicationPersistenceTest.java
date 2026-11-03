package com.example.job_tracker.jobapplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.job_tracker.TestcontainersConfiguration;
import com.example.job_tracker.company.Company;
import com.example.job_tracker.company.CompanyRepository;
import com.example.job_tracker.jobapplication.model.JobSource;
import com.example.job_tracker.jobapplication.model.RecruitmentStatus;
import com.example.job_tracker.jobapplication.model.SalaryBasis;
import com.example.job_tracker.jobapplication.model.SalaryPeriod;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;


/**
 * Test integracyjny na prawdziwym Postgresie (Testcontainers).
 * Samo załadowanie kontekstu sprawdza też zgodność encji ze schematem Flyway (ddl-auto=validate).
 * TestcontainersConfiguration generuje Spring Initializr; jeśli ma u Ciebie inną nazwę/pakiet, popraw import.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class JobApplicationPersistenceTest {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private JobApplicationRepository jobApplicationRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void savesAndLoadsApplicationWithSalaryAndDefaults() {
        Company company = companyRepository.save(new Company("Acme Sp. z o.o."));

        JobApplication application = new JobApplication(company, "Junior Java Developer");
        application.setSalary(new SalaryRange(
                new BigDecimal("8000"), new BigDecimal("12000"), "PLN", SalaryPeriod.MONTH, SalaryBasis.GROSS));
        jobApplicationRepository.save(application);

        entityManager.flush();
        entityManager.clear();

        JobApplication loaded = jobApplicationRepository.findById(application.getId()).orElseThrow();

        assertThat(loaded.getCompany().getName()).isEqualTo("Acme Sp. z o.o.");
        assertThat(loaded.getCurrentStatus()).isEqualTo(RecruitmentStatus.FOUND);
        assertThat(loaded.getSource()).isEqualTo(JobSource.OTHER);
        assertThat(loaded.getDateFound()).isNotNull();
        assertThat(loaded.getSalary().getMin()).isEqualByComparingTo("8000");
        assertThat(loaded.getSalary().getBasis()).isEqualTo(SalaryBasis.GROSS);
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getUpdatedAt()).isNotNull();
    }

    @Test
    void rejectsCompanyNameDifferingOnlyByCase() {
        companyRepository.saveAndFlush(new Company("Acme"));

        assertThatThrownBy(() -> companyRepository.saveAndFlush(new Company("ACME")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsSalaryRangeWithMinGreaterThanMax() {
        assertThatThrownBy(() -> new SalaryRange(
                new BigDecimal("12000"), new BigDecimal("8000"), "PLN", SalaryPeriod.MONTH, SalaryBasis.NET))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
