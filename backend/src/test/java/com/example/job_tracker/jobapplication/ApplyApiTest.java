package com.example.job_tracker.jobapplication;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import com.example.job_tracker.TestcontainersConfiguration;
import com.example.job_tracker.auth.SecurityProperties;
import com.jayway.jsonpath.JsonPath;
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
 * POST /api/applications/{id}/apply: APPLIED entry, dateApplied and documents in one step.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class ApplyApiTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private Clock clock;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private SecurityProperties securityProperties;

    private MockMvc mockMvc;
    private long companyId;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        companyId = createCompany("Apply Co");
    }

    @Test
    void anEmptyBodyAppliesNow() throws Exception {
        long id = createApplication(null);

        mockMvc.perform(post("/api/applications/{id}/apply", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStatus").value("APPLIED"))
                .andExpect(jsonPath("$.dateApplied").value(LocalDate.now(clock).toString()))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.lastStatusChangedAt").exists());

        mockMvc.perform(get("/api/applications/{id}/history", id))
                .andExpect(jsonPath("$[*].status").value(org.hamcrest.Matchers.contains("FOUND", "APPLIED")));
    }

    @Test
    void appliedAtNoteAndDocumentsAreStored() throws Exception {
        long id = createApplication("2026-11-01");
        long cv = createDocument("CV_Apply", "CV");
        long letter = createDocument("Letter_Apply", "COVER_LETTER");
        mockMvc.perform(put("/api/applications/{id}/documents/{documentId}", id, cv))
                .andExpect(status().isNoContent()); // already attached: attaching again changes nothing

        String appliedAt = "2026-11-10T09:30:00Z";
        mockMvc.perform(post("/api/applications/{id}/apply", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appliedAt\":\"" + appliedAt + "\",\"note\":\"Sent via the website\","
                                + "\"documentIds\":[" + cv + "," + letter + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dateApplied").value(
                        LocalDate.ofInstant(Instant.parse(appliedAt), clock.getZone()).toString()))
                .andExpect(jsonPath("$.lastStatusChangedAt").value(appliedAt));

        mockMvc.perform(get("/api/applications/{id}/history", id))
                .andExpect(jsonPath("$[1].status").value("APPLIED"))
                .andExpect(jsonPath("$[1].changedAt").value(appliedAt))
                .andExpect(jsonPath("$[1].note").value("Sent via the website"));
        mockMvc.perform(get("/api/applications/{id}/documents", id))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].id").value(containsInAnyOrder((int) cv, (int) letter)));
    }

    @Test
    void applyingTwiceIsAConflict() throws Exception {
        long id = createApplication(null);
        mockMvc.perform(post("/api/applications/{id}/apply", id)).andExpect(status().isOk());

        mockMvc.perform(post("/api/applications/{id}/apply", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.startsWith(
                        "The application is already applied")));
        mockMvc.perform(get("/api/applications/{id}/history", id))
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void anApplicationPastFoundWithoutAnAppliedEntryIsAConflict() throws Exception {
        long id = createApplication(null);
        mockMvc.perform(post("/api/applications/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"HR_SCREEN\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/applications/{id}/apply", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("HR_SCREEN")));
        mockMvc.perform(get("/api/applications/{id}", id))
                .andExpect(jsonPath("$.currentStatus").value("HR_SCREEN"));
    }

    @Test
    void anArchivedDocumentStopsTheWholeOperation() throws Exception {
        long id = createApplication(null);
        long archived = createDocument("Old_CV", "CV");
        mockMvc.perform(put("/api/documents/{id}", archived).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Old_CV\",\"type\":\"CV\",\"archived\":true}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/applications/{id}/apply", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentIds\":[" + archived + "]}"))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/applications/{id}", id))
                .andExpect(jsonPath("$.currentStatus").value("FOUND"))
                .andExpect(jsonPath("$.dateApplied").doesNotExist());
        mockMvc.perform(get("/api/applications/{id}/documents", id))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void invalidDatesAreBadRequests() throws Exception {
        long id = createApplication("2026-11-01");

        mockMvc.perform(post("/api/applications/{id}/apply", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appliedAt\":\"2026-10-15T10:00:00Z\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("dateApplied cannot be before dateFound"));
        mockMvc.perform(post("/api/applications/{id}/apply", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appliedAt\":\"2099-01-01T10:00:00Z\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.appliedAt").exists());
    }

    @Test
    void missingResourcesAreNotFound() throws Exception {
        long id = createApplication(null);

        mockMvc.perform(post("/api/applications/{id}/apply", 999_999))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/applications/{id}/apply", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentIds\":[999999]}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/applications/{id}", id))
                .andExpect(jsonPath("$.currentStatus").value("FOUND"));
    }

    @Test
    void onlyAdminCanApply() throws Exception {
        long id = createApplication(null);
        MockMvc secured = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        secured.perform(post("/api/applications/{id}/apply", id).header(HttpHeaders.AUTHORIZATION, bearerFor("DEMO")))
                .andExpect(status().isForbidden());
        secured.perform(post("/api/applications/{id}/apply", id).header(HttpHeaders.AUTHORIZATION, bearerFor("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStatus").value("APPLIED"));
    }

    private String bearerFor(String role) {
        // Real time: the resource server validates expiry against the system clock
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

    private long createCompany(String name) throws Exception {
        String body = mockMvc.perform(post("/api/companies").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long createApplication(String dateFound) throws Exception {
        String date = dateFound == null ? "" : ",\"dateFound\":\"" + dateFound + "\"";
        String body = mockMvc.perform(post("/api/applications").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyId\":" + companyId + ",\"position\":\"Java Developer\"" + date + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long createDocument(String name, String type) throws Exception {
        String body = mockMvc.perform(post("/api/documents").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"type\":\"" + type + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}
