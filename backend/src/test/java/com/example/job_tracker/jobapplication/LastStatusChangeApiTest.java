package com.example.job_tracker.jobapplication;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.job_tracker.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * lastStatusChangedAt: the time of the most recent history entry, FOUND included.
 * The test configuration uses the default time zone (UTC), so start-of-day instants end with T00:00:00Z.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class LastStatusChangeApiTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private EntityManager entityManager;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void aNewApplicationReportsItsFoundEntry() throws Exception {
        long companyId = createCompany("Found Co");

        mockMvc.perform(post("/api/applications").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyId\":" + companyId + ",\"position\":\"Dev\",\"dateFound\":\"2026-11-01\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lastStatusChangedAt").value("2026-11-01T00:00:00Z"));

        mockMvc.perform(get("/api/applications").param("companyId", String.valueOf(companyId)))
                .andExpect(jsonPath("$.content[0].lastStatusChangedAt").value("2026-11-01T00:00:00Z"));
    }

    @Test
    void itFollowsTheLatestEntryAndIgnoresBackdatedOnes() throws Exception {
        long id = createApplication(createCompany("Status Co"), "2026-11-01");

        changeStatus(id, "APPLIED", "2026-11-10T08:00:00Z");
        changeStatus(id, "HR_SCREEN", "2026-11-03T08:00:00Z"); // backdated, not the latest
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/applications/{id}", id))
                .andExpect(jsonPath("$.lastStatusChangedAt").value("2026-11-10T08:00:00Z"));
    }

    @Test
    void anUpdateThatMovesDateAppliedReturnsTheNewValue() throws Exception {
        long companyId = createCompany("Update Co");
        long id = createApplication(companyId, "2026-11-01");

        mockMvc.perform(put("/api/applications/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyId\":" + companyId + ",\"position\":\"Dev\",\"source\":\"OTHER\","
                                + "\"dateFound\":\"2026-11-01\",\"dateApplied\":\"2026-11-05\",\"version\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastStatusChangedAt").value("2026-11-05T00:00:00Z"));
    }

    @Test
    void theListCanBeSortedByLastStatusChange() throws Exception {
        long companyId = createCompany("Sort Co");
        long oldest = createApplication(companyId, "2026-10-01");
        long newest = createApplication(companyId, "2026-10-02");
        long middle = createApplication(companyId, "2026-10-03");
        changeStatus(newest, "APPLIED", "2026-11-20T10:00:00Z");
        changeStatus(middle, "APPLIED", "2026-11-10T10:00:00Z");
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/applications").param("companyId", String.valueOf(companyId))
                        .param("sort", "lastStatusChangedAt,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id").value(contains((int) oldest, (int) middle, (int) newest)));
        mockMvc.perform(get("/api/applications").param("companyId", String.valueOf(companyId))
                        .param("sort", "lastStatusChangedAt,desc"))
                .andExpect(jsonPath("$.content[*].id").value(contains((int) newest, (int) middle, (int) oldest)));
    }

    private void changeStatus(long id, String status, String changedAt) throws Exception {
        mockMvc.perform(post("/api/applications/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + status + "\",\"changedAt\":\"" + changedAt + "\"}"))
                .andExpect(status().isCreated());
    }

    private long createCompany(String name) throws Exception {
        String body = mockMvc.perform(post("/api/companies").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long createApplication(long companyId, String dateFound) throws Exception {
        String body = mockMvc.perform(post("/api/applications").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyId\":" + companyId + ",\"position\":\"Java Developer\","
                                + "\"dateFound\":\"" + dateFound + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}
