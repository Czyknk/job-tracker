package com.example.job_tracker.jobapplication;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


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
import org.testcontainers.utility.TestcontainersConfiguration;

/**
 * Test warstwy HTTP na pełnym kontekście i prawdziwym Postgresie.
 * MockMvc budujemy ręcznie z WebApplicationContext, żeby nie zależeć od pakietów adnotacji slice'owych.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class JobApplicationApiTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void createsApplicationAndTracksStatusThroughApi() throws Exception {
        long companyId = createCompany("Initech");
        long applicationId = createApplication(companyId, "Junior Java Developer");

        mockMvc.perform(post("/api/applications/{id}/status", applicationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPLIED\",\"note\":\"Sent CV\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPLIED"))
                .andExpect(jsonPath("$.note").value("Sent CV"));

        mockMvc.perform(get("/api/applications/{id}", applicationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Initech"))
                .andExpect(jsonPath("$.currentStatus").value("APPLIED"))
                .andExpect(jsonPath("$.dateApplied").isNotEmpty())
                .andExpect(jsonPath("$.salary.min").value(8000))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        mockMvc.perform(get("/api/applications/{id}/history", applicationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("FOUND"))
                .andExpect(jsonPath("$[1].status").value("APPLIED"));
    }

    @Test
    void filtersApplicationsByStatusAndCompany() throws Exception {
        long companyId = createCompany("Hooli");
        long first = createApplication(companyId, "Backend Developer");
        createApplication(companyId, "Java Developer");

        mockMvc.perform(post("/api/applications/{id}/status", first)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPLIED\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/applications")
                        .param("status", "APPLIED")
                        .param("companyId", String.valueOf(companyId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].position").value("Backend Developer"));

        mockMvc.perform(get("/api/applications").param("companyId", String.valueOf(companyId)))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void returnsValidationErrorsForInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"position\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.companyId").exists())
                .andExpect(jsonPath("$.errors.position").exists());
    }

    @Test
    void rejectsSalaryWithMinGreaterThanMax() throws Exception {
        long companyId = createCompany("Umbrella");

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyId\":" + companyId + ",\"position\":\"Dev\","
                                + "\"salary\":{\"min\":12000,\"max\":8000,\"currency\":\"PLN\"}}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsNotFoundForUnknownApplicationAndCompany() throws Exception {
        mockMvc.perform(get("/api/applications/{id}", 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyId\":999999,\"position\":\"Dev\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsConflictWhenChangingFinalStatus() throws Exception {
        long applicationId = createApplication(createCompany("Wayne"), "Developer");

        mockMvc.perform(post("/api/applications/{id}/status", applicationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REJECTED\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/applications/{id}/status", applicationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"TECH_INTERVIEW\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void returnsConflictForDuplicateCompanyName() throws Exception {
        createCompany("Stark");

        mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"STARK\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsUnsupportedSortProperty() throws Exception {
        mockMvc.perform(get("/api/applications").param("sort", "nonexistent,asc"))
                .andExpect(status().isBadRequest());
    }

    private long createCompany(String name) throws Exception {
        String body = mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long createApplication(long companyId, String position) throws Exception {
        String body = mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyId\":" + companyId + ",\"position\":\"" + position + "\","
                                + "\"source\":\"PRACUJ_PL\",\"workMode\":\"REMOTE\","
                                + "\"salary\":{\"min\":8000,\"max\":12000,\"currency\":\"PLN\","
                                + "\"period\":\"MONTH\",\"basis\":\"GROSS\"}}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}
