package com.example.job_tracker.jobapplication;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.job_tracker.TestcontainersConfiguration;
import com.example.job_tracker.company.Company;
import com.example.job_tracker.company.CompanyRepository;
import com.example.job_tracker.jobapplication.history.StatusHistory;
import com.example.job_tracker.jobapplication.model.RecruitmentStatus;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class JobApplicationServiceIntegrationTest {

    @Autowired
    private JobApplicationService service;

    @Autowired
    private JobApplicationRepository applicationRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void tracksRecruitmentPathInHistoryAndCurrentStatus() {
        Company company = companyRepository.save(new Company("Globex"));
        JobApplication application = service.create(new JobApplication(company, "Backend Developer"));

        service.changeStatus(application.getId(), RecruitmentStatus.APPLIED, null, "Sent CV");
        service.changeStatus(application.getId(), RecruitmentStatus.HR_SCREEN, null, null);

        entityManager.flush();
        entityManager.clear();

        List<StatusHistory> history = service.getHistory(application.getId());
        assertThat(history).extracting(StatusHistory::getStatus)
                .containsExactly(RecruitmentStatus.FOUND, RecruitmentStatus.APPLIED, RecruitmentStatus.HR_SCREEN);

        JobApplication reloaded = applicationRepository.findById(application.getId()).orElseThrow();
        assertThat(reloaded.getCurrentStatus()).isEqualTo(RecruitmentStatus.HR_SCREEN);
        assertThat(reloaded.getDateApplied()).isNotNull();
    }
}
