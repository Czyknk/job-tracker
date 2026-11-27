package com.example.job_tracker.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Map;

import com.example.job_tracker.FixedClockConfiguration;
import com.example.job_tracker.TestcontainersConfiguration;
import com.example.job_tracker.auth.SecurityProperties;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * GET /api/dashboard with a fixed clock (2026-11-28 00:30 in Warsaw) and a small limit (2) to check the caps.
 */
@SpringBootTest(properties = {
        "app.dashboard.stale-days=14",
        "app.dashboard.deadline-days=7",
        "app.dashboard.limit=2"})
@Import({TestcontainersConfiguration.class, FixedClockConfiguration.class})
@Transactional
class DashboardApiTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private SecurityProperties securityProperties;

    private MockMvc mockMvc;
    private long companyId;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        companyId = createCompany("Dashboard Co");
    }

    @Test
    void anEmptyDatabaseGivesZeroCountsAndEmptyLists() throws Exception {
        String body = mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.needsAttention", hasSize(0)))
                .andExpect(jsonPath("$.upcoming", hasSize(0)))
                .andExpect(jsonPath("$.recentActivity", hasSize(0)))
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> counts = JsonPath.read(body, "$.activeByStatus");
        assertThat(counts).containsExactly(
                Map.entry("FOUND", 0), Map.entry("APPLIED", 0), Map.entry("HR_SCREEN", 0),
                Map.entry("TECH_TASK", 0), Map.entry("TECH_INTERVIEW", 0), Map.entry("FINAL_INTERVIEW", 0),
                Map.entry("OFFER_RECEIVED", 0));
    }

    @Test
    void theDashboardCollectsWhatNeedsAttentionToday() throws Exception {
        long foundOld = create("Found old", "\"dateFound\":\"2026-11-01\","
                + "\"nextStepAt\":\"2026-11-27T10:00:00Z\""); // a next step yesterday (Warsaw) is not upcoming
        long foundNew = create("Found new", "\"dateFound\":\"2026-11-20\"");
        create("Found newest", "\"dateFound\":\"2026-11-25\""); // over the limit of 2
        long stale = create("Stale", "\"dateFound\":\"2026-11-01\",\"dateApplied\":\"2026-11-10\","
                + "\"deadline\":\"2026-12-01\",\"nextStepAt\":\"2026-12-03T09:00:00Z\"");
        long interview = create("Interview", "\"dateFound\":\"2026-11-15\",\"dateApplied\":\"2026-11-20\","
                + "\"nextStepAt\":\"2026-11-28T08:00:00Z\""); // later today in Warsaw
        changeStatus(interview, "HR_SCREEN");
        long ghosted = create("Ghosted", "\"dateFound\":\"2026-10-01\",\"dateApplied\":\"2026-10-02\","
                + "\"deadline\":\"2026-11-30\",\"nextStepAt\":\"2026-12-01T09:00:00Z\"");
        changeStatus(ghosted, "GHOSTED");
        long archived = create("Archived", "\"dateFound\":\"2026-10-01\",\"dateApplied\":\"2026-10-02\","
                + "\"deadline\":\"2026-11-29\",\"nextStepAt\":\"2026-12-01T09:00:00Z\"");
        mockMvc.perform(post("/api/applications/{id}/archive", archived)).andExpect(status().isOk());
        entityManager.flush();
        entityManager.clear();

        String body = mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.needsAttention[*].reason")
                        .value(contains("DEADLINE_SOON", "NO_RESPONSE", "NOT_APPLIED_YET", "NOT_APPLIED_YET")))
                .andExpect(jsonPath("$.needsAttention[*].application.id")
                        .value(contains((int) stale, (int) stale, (int) foundOld, (int) foundNew)))
                .andExpect(jsonPath("$.needsAttention[1].application.lastStatusChangedAt").exists())
                .andExpect(jsonPath("$.upcoming[*].id").value(contains((int) interview, (int) stale)))
                .andExpect(jsonPath("$.upcoming[0].companyName").value("Dashboard Co"))
                // both changes happened "now"; the later entry comes first
                .andExpect(jsonPath("$.recentActivity[*].applicationId").value(contains((int) ghosted, (int) interview)))
                .andExpect(jsonPath("$.recentActivity[0].status").value("GHOSTED"))
                .andExpect(jsonPath("$.recentActivity[0].position").value("Ghosted"))
                .andExpect(jsonPath("$.recentActivity[0].companyName").value("Dashboard Co"))
                .andExpect(jsonPath("$.recentActivity[0].changedAt").value(FixedClockConfiguration.NOW.toString()))
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> counts = JsonPath.read(body, "$.activeByStatus");
        assertThat(counts).containsEntry("FOUND", 3).containsEntry("APPLIED", 1).containsEntry("HR_SCREEN", 1)
                .doesNotContainKeys("GHOSTED", "REJECTED", "ACCEPTED", "WITHDRAWN");
    }

    @Test
    void everyRoleCanReadTheDashboardButNobodyAnonymous() throws Exception {
        MockMvc secured = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        secured.perform(get("/api/dashboard").header(HttpHeaders.AUTHORIZATION, bearerFor("DEMO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeByStatus").exists());
        secured.perform(get("/api/dashboard").header(HttpHeaders.AUTHORIZATION, bearerFor("ADMIN")))
                .andExpect(status().isOk());
        secured.perform(get("/api/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        // writes stay ADMIN-only, whatever the path
        secured.perform(post("/api/dashboard").header(HttpHeaders.AUTHORIZATION, bearerFor("DEMO")))
                .andExpect(status().isForbidden());
    }

    private String bearerFor(String role) {
        // Real time, not the fixed clock: the resource server validates expiry against the system clock
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(securityProperties.jwt().issuer())
                .subject("1")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(600))
                .claim("role", role)
                .build();
        return "Bearer " + jwtEncoder.encode(
                JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    private void changeStatus(long id, String status) throws Exception {
        mockMvc.perform(post("/api/applications/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + status + "\"}"))
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
        String body = mockMvc.perform(post("/api/applications").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyId\":" + companyId + ",\"position\":\"" + position + "\","
                                + extraFields + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}
