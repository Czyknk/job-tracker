package com.example.job_tracker.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.job_tracker.TestcontainersConfiguration;
import com.example.job_tracker.summary.OfferSummaryRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Starts a whole application in demo mode (no admin account, sample data seeded at startup) and checks
 * what a visitor can do. This context needs a database without an ADMIN user, which holds as long as
 * each Spring context gets its own throwaway PostgreSQL container.
 */
@SpringBootTest(properties = {
        "app.demo.enabled=true",
        "app.security.admin.email=",
        "app.security.admin.password="
})
@Import(TestcontainersConfiguration.class)
class DemoModeApiTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private OfferSummaryRepository summaryRepository;

    private MockMvc mockMvc;
    private String demoToken;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"demo@job-tracker.example\",\"password\":\"demo-readonly-pass\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        demoToken = "Bearer " + JsonPath.read(body, "$.accessToken");
    }

    @Test
    void sampleDataIsAvailableToTheDemoUser() throws Exception {
        mockMvc.perform(get("/api/applications").param("size", "50").header(HttpHeaders.AUTHORIZATION, demoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(6)));

        mockMvc.perform(get("/api/companies").header(HttpHeaders.AUTHORIZATION, demoToken))
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(5)));

        mockMvc.perform(get("/api/contacts").header(HttpHeaders.AUTHORIZATION, demoToken))
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(4)));

        // two active documents; the archived v1 is hidden by default
        mockMvc.perform(get("/api/documents").header(HttpHeaders.AUTHORIZATION, demoToken))
                .andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(get("/api/documents").param("includeArchived", "true")
                        .header(HttpHeaders.AUTHORIZATION, demoToken))
                .andExpect(jsonPath("$.length()").value(3));

        assertThat(summaryRepository.count()).isPositive();
    }

    @Test
    void everyApplicationHasAStatusHistory() throws Exception {
        String body = mockMvc.perform(get("/api/applications").param("size", "50")
                        .header(HttpHeaders.AUTHORIZATION, demoToken))
                .andReturn().getResponse().getContentAsString();
        java.util.List<Integer> ids = JsonPath.read(body, "$.content[*].id");

        assertThat(ids).isNotEmpty();
        for (Integer id : ids) {
            mockMvc.perform(get("/api/applications/{id}/history", id).header(HttpHeaders.AUTHORIZATION, demoToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)));
        }
    }

    @Test
    void theDemoUserCannotModifyAnythingOrCallTheAiProvider() throws Exception {
        mockMvc.perform(post("/api/companies")
                        .header(HttpHeaders.AUTHORIZATION, demoToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Vandal Inc.\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(post("/api/applications/1/summaries").header(HttpHeaders.AUTHORIZATION, demoToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void thereIsNoOwnerAccountOnADemoInstance() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@example.com\",\"password\":\"test-admin-password\"}"))
                .andExpect(status().isUnauthorized());
    }
}
