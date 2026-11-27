package com.example.job_tracker.jobapplication;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.job_tracker.FixedClockConfiguration;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * List filters: active, multi-value status, staleDays and deadlineWithinDays.
 * The clock is fixed at 2026-11-28 00:30 in Warsaw (still 2026-11-27 in UTC), see {@link FixedClockConfiguration}.
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, FixedClockConfiguration.class})
@Transactional
class ApplicationFilterApiTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private EntityManager entityManager;

    private MockMvc mockMvc;
    private long companyId;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        companyId = createCompany("Filter Co");
    }

    @Test
    void activeSeparatesOpenFromFinishedAndArchivedApplications() throws Exception {
        long found = create("Found", "\"dateFound\":\"2026-11-20\"");
        long applied = create("Applied", "\"dateFound\":\"2026-11-20\",\"dateApplied\":\"2026-11-21\"");
        long ghosted = create("Ghosted", "\"dateFound\":\"2026-11-20\",\"dateApplied\":\"2026-11-21\"");
        changeStatus(ghosted, "GHOSTED");
        long rejected = create("Rejected", "\"dateFound\":\"2026-11-20\",\"dateApplied\":\"2026-11-21\"");
        changeStatus(rejected, "REJECTED");
        long archived = create("Archived", "\"dateFound\":\"2026-11-20\",\"dateApplied\":\"2026-11-21\"");
        mockMvc.perform(post("/api/applications/{id}/archive", archived)).andExpect(status().isOk());
        reload();

        expectIds(list().param("active", "true"), found, applied);
        expectIds(list().param("active", "false"), ghosted, rejected, archived);
        // without active/archived the default is unchanged: archived applications are hidden, finished ones are not
        expectIds(list(), found, applied, ghosted, rejected);
        // explicit filters are combined with AND
        expectIds(list().param("active", "true").param("archived", "ARCHIVED"));
        expectIds(list().param("active", "false").param("archived", "ACTIVE"), ghosted, rejected);
    }

    @Test
    void statusAcceptsSeveralValues() throws Exception {
        long found = create("Found", "\"dateFound\":\"2026-11-20\"");
        long applied = create("Applied", "\"dateFound\":\"2026-11-20\",\"dateApplied\":\"2026-11-21\"");
        long ghosted = create("Ghosted", "\"dateFound\":\"2026-11-20\",\"dateApplied\":\"2026-11-21\"");
        changeStatus(ghosted, "GHOSTED");
        reload();

        expectIds(list().param("status", "APPLIED").param("status", "GHOSTED"), applied, ghosted);
        expectIds(list().param("status", "APPLIED"), applied);
        expectIds(list().param("status", "FOUND").param("status", "GHOSTED").param("active", "true"), found);
    }

    @Test
    void staleDaysFindsApplicationsWaitingForTheEmployer() throws Exception {
        // today is 2026-11-28 in Warsaw: with staleDays=14 the last change must be on or before 2026-11-14
        long exactlyStale = create("Exactly 14 days", "\"dateFound\":\"2026-11-01\",\"dateApplied\":\"2026-11-14\"");
        create("13 days", "\"dateFound\":\"2026-11-01\",\"dateApplied\":\"2026-11-15\"");
        long olderStage = create("Old HR screen", "\"dateFound\":\"2026-10-01\",\"dateApplied\":\"2026-10-02\"");
        changeStatus(olderStage, "HR_SCREEN", "2026-10-10T10:00:00Z");
        long recentStage = create("Recent HR screen", "\"dateFound\":\"2026-10-01\",\"dateApplied\":\"2026-10-02\"");
        changeStatus(recentStage, "HR_SCREEN", "2026-11-25T10:00:00Z");
        create("Never applied", "\"dateFound\":\"2026-10-01\"");
        long ghosted = create("Ghosted", "\"dateFound\":\"2026-10-01\",\"dateApplied\":\"2026-10-02\"");
        changeStatus(ghosted, "GHOSTED", "2026-10-20T10:00:00Z");
        long archived = create("Archived", "\"dateFound\":\"2026-10-01\",\"dateApplied\":\"2026-10-02\"");
        mockMvc.perform(post("/api/applications/{id}/archive", archived)).andExpect(status().isOk());
        reload();

        expectIds(list().param("staleDays", "14"), exactlyStale, olderStage);
        expectIds(list().param("staleDays", "14").param("q", "screen"), olderStage);
    }

    @Test
    void deadlineWithinDaysUsesTodayInTheApplicationTimeZone() throws Exception {
        create("Yesterday in Warsaw, today in UTC", "\"deadline\":\"2026-11-27\"");
        long today = create("Today", "\"deadline\":\"2026-11-28\"");
        long lastDay = create("Last day", "\"deadline\":\"2026-12-05\"");
        create("Too late", "\"deadline\":\"2026-12-06\"");
        create("No deadline", null);
        reload();

        expectIds(list().param("deadlineWithinDays", "7"), today, lastDay);
        expectIds(list().param("deadlineWithinDays", "0"), today);
        expectIds(list().param("deadlineWithinDays", "7").param("deadlineBefore", "2026-12-01"), today);
    }

    @Test
    void invalidDayCountsAreBadRequests() throws Exception {
        mockMvc.perform(list().param("staleDays", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.staleDays").exists());
        mockMvc.perform(list().param("deadlineWithinDays", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.deadlineWithinDays").exists());
    }

    private MockHttpServletRequestBuilder list() {
        return get("/api/applications").param("companyId", String.valueOf(companyId)).param("size", "50");
    }

    private void expectIds(MockHttpServletRequestBuilder request, long... ids) throws Exception {
        ResultActions result = mockMvc.perform(request).andExpect(status().isOk());
        if (ids.length == 0) {
            result.andExpect(jsonPath("$.content", hasSize(0)));
            return;
        }
        Integer[] expected = java.util.Arrays.stream(ids).mapToObj(id -> (int) id).toArray(Integer[]::new);
        result.andExpect(jsonPath("$.content[*].id").value(containsInAnyOrder(expected)));
    }

    // The derived lastStatusChangedAt is read on load, so make the next requests read fresh rows
    private void reload() {
        entityManager.flush();
        entityManager.clear();
    }

    private void changeStatus(long id, String status) throws Exception {
        mockMvc.perform(post("/api/applications/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + status + "\"}"))
                .andExpect(status().isCreated());
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

    private long create(String position, String extraFields) throws Exception {
        String extra = extraFields == null ? "" : "," + extraFields;
        String body = mockMvc.perform(post("/api/applications").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyId\":" + companyId + ",\"position\":\"" + position + "\"" + extra + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}
