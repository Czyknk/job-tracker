package com.example.job_tracker.jobapplication;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.job_tracker.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
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
 * Lean list items, free-text search and filters. Lists are always narrowed to one company, so the
 * tests do not depend on other data.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class ApplicationSearchApiTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void listItemsAreLeanAndTheListIsSearchable() throws Exception {
        long companyId = createCompany("Searchable Co");
        createApplication(companyId, "Spring Boot Developer", "OTHER", null, "Work with PostgreSQL and Kafka");
        createApplication(companyId, "Frontend Developer", "OTHER", null, "React and TypeScript");
        String company = String.valueOf(companyId);

        mockMvc.perform(get("/api/applications").param("companyId", company).param("q", "postgres"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].position").value("Spring Boot Developer"))
                .andExpect(jsonPath("$.content[0].rawDescription").doesNotExist())
                .andExpect(jsonPath("$.content[0].notes").doesNotExist());

        // every word must match somewhere
        mockMvc.perform(get("/api/applications").param("companyId", company).param("q", "developer kafka"))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/applications").param("companyId", company).param("q", "REACT"))
                .andExpect(jsonPath("$.content[0].position").value("Frontend Developer"));
        // the company name is searched too
        mockMvc.perform(get("/api/applications").param("companyId", company).param("q", "searchable"))
                .andExpect(jsonPath("$.totalElements").value(2));
        // LIKE wildcards typed by the user are escaped, not interpreted
        mockMvc.perform(get("/api/applications").param("companyId", company).param("q", "%"))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void listCanBeFilteredBySourceAndWorkMode() throws Exception {
        long companyId = createCompany("Filter Co");
        createApplication(companyId, "Remote Dev", "LINKEDIN", "REMOTE", null);
        createApplication(companyId, "Office Dev", "PRACUJ_PL", "ONSITE", null);
        String company = String.valueOf(companyId);

        mockMvc.perform(get("/api/applications").param("companyId", company).param("source", "LINKEDIN"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].position").value("Remote Dev"));
        mockMvc.perform(get("/api/applications").param("companyId", company).param("workMode", "ONSITE"))
                .andExpect(jsonPath("$.content[0].position").value("Office Dev"));
        mockMvc.perform(get("/api/applications").param("companyId", company).param("source", "NOT_A_SOURCE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listCanBeFilteredByStatus() throws Exception {
        long companyId = createCompany("Status Filter Co");
        long applied = createApplication(companyId, "Applied Dev", "OTHER", null, null);
        createApplication(companyId, "Found Dev", "OTHER", null, null);
        mockMvc.perform(post("/api/applications/{id}/status", applied).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPLIED\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/applications").param("companyId", String.valueOf(companyId))
                        .param("status", "APPLIED"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].position").value("Applied Dev"));
    }

    private long createCompany(String name) throws Exception {
        String body = mockMvc.perform(post("/api/companies").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long createApplication(long companyId, String position, String source, String workMode,
                                   String description) throws Exception {
        StringBuilder json = new StringBuilder("{\"companyId\":" + companyId + ",\"position\":\"" + position
                + "\",\"source\":\"" + source + "\"");
        if (workMode != null) {
            json.append(",\"workMode\":\"").append(workMode).append("\"");
        }
        if (description != null) {
            json.append(",\"rawDescription\":\"").append(description).append("\"");
        }
        json.append("}");
        String body = mockMvc.perform(post("/api/applications").contentType(MediaType.APPLICATION_JSON)
                        .content(json.toString()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}
