package com.example.job_tracker.jobapplication;

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
 * Notes and the version field (optimistic locking) of an application.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class ApplicationVersioningApiTest {

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
    void anUpdateCarriesTheNotesAndBumpsTheVersion() throws Exception {
        long companyId = createCompany("Notes Co");
        long id = createApplication(companyId);

        mockMvc.perform(get("/api/applications/{id}", id))
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.archived").value(false))
                .andExpect(jsonPath("$.notes").doesNotExist());

        mockMvc.perform(put("/api/applications/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(companyId, 0, "  Ask about the testing culture  ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value("Ask about the testing culture"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void aStaleVersionIsRejectedWithConflict() throws Exception {
        long companyId = createCompany("Lock Co");
        long id = createApplication(companyId);

        // the phone saves first
        mockMvc.perform(put("/api/applications/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(companyId, 0, "edited on the phone")))
                .andExpect(status().isOk());
        entityManager.flush();
        entityManager.clear();

        // the computer still holds version 0
        mockMvc.perform(put("/api/applications/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(companyId, 0, "edited on the computer")))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/applications/{id}", id))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.notes").value("edited on the phone"));
        mockMvc.perform(put("/api/applications/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(companyId, 1, "merged by hand")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    void anUpdateWithoutAVersionIsABadRequest() throws Exception {
        long companyId = createCompany("Missing Version Co");
        long id = createApplication(companyId);

        mockMvc.perform(put("/api/applications/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyId\":" + companyId + ",\"position\":\"Dev\",\"source\":\"OTHER\","
                                + "\"dateFound\":\"2026-11-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.version").exists());
    }

    private static String updateBody(long companyId, long version, String notes) {
        return "{\"companyId\":" + companyId + ",\"position\":\"Updated\",\"source\":\"OTHER\","
                + "\"dateFound\":\"2026-11-01\",\"notes\":\"" + notes + "\",\"version\":" + version + "}";
    }

    private long createCompany(String name) throws Exception {
        String body = mockMvc.perform(post("/api/companies").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long createApplication(long companyId) throws Exception {
        String body = mockMvc.perform(post("/api/applications").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyId\":" + companyId + ",\"position\":\"Java Developer\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}
