package com.example.job_tracker.jobapplication;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
 * The optional "next step" of an application (nextStepAt, nextStepNote).
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class NextStepApiTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void theNextStepIsStoredAndReturnedInDetailAndList() throws Exception {
        long companyId = createCompany("Next Step Co");

        String body = mockMvc.perform(post("/api/applications").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyId\":" + companyId + ",\"position\":\"Java Developer\","
                                + "\"nextStepAt\":\"2026-12-02T09:00:00Z\","
                                + "\"nextStepNote\":\"  Technical interview  \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nextStepAt").value("2026-12-02T09:00:00Z"))
                .andExpect(jsonPath("$.nextStepNote").value("Technical interview"))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(body, "$.id")).longValue();

        mockMvc.perform(get("/api/applications/{id}", id))
                .andExpect(jsonPath("$.nextStepAt").value("2026-12-02T09:00:00Z"));
        mockMvc.perform(get("/api/applications").param("companyId", String.valueOf(companyId)))
                .andExpect(jsonPath("$.content[0].nextStepAt").value("2026-12-02T09:00:00Z"))
                .andExpect(jsonPath("$.content[0].nextStepNote").value("Technical interview"));
    }

    @Test
    void aStatusChangeDoesNotTouchTheNextStep() throws Exception {
        long id = createApplication(createCompany("Status Co"), "\"nextStepAt\":\"2026-12-02T09:00:00Z\"");

        mockMvc.perform(post("/api/applications/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPLIED\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/applications/{id}", id))
                .andExpect(jsonPath("$.nextStepAt").value("2026-12-02T09:00:00Z"));
    }

    @Test
    void anUpdateCanSetAndClearTheNextStep() throws Exception {
        long companyId = createCompany("Update Co");
        long id = createApplication(companyId, null);

        mockMvc.perform(put("/api/applications/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(companyId, 0,
                                ",\"nextStepAt\":\"2026-12-05T14:30:00Z\",\"nextStepNote\":\"Send the task\"")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextStepAt").value("2026-12-05T14:30:00Z"))
                .andExpect(jsonPath("$.nextStepNote").value("Send the task"))
                .andExpect(jsonPath("$.version").value(1));

        // PUT semantics: omitting the optional fields clears them
        mockMvc.perform(put("/api/applications/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(companyId, 1, "")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextStepAt").doesNotExist())
                .andExpect(jsonPath("$.nextStepNote").doesNotExist())
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    void aStaleVersionIsStillRejected() throws Exception {
        long companyId = createCompany("Stale Co");
        long id = createApplication(companyId, null);

        mockMvc.perform(put("/api/applications/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(companyId, 7, ",\"nextStepAt\":\"2026-12-05T14:30:00Z\"")))
                .andExpect(status().isConflict());
    }

    @Test
    void aTooLongNoteIsABadRequest() throws Exception {
        long companyId = createCompany("Long Note Co");

        mockMvc.perform(post("/api/applications").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyId\":" + companyId + ",\"position\":\"Dev\",\"nextStepNote\":\""
                                + "x".repeat(201) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.nextStepNote").exists());
    }

    private static String updateBody(long companyId, long version, String extraFields) {
        return "{\"companyId\":" + companyId + ",\"position\":\"Updated\",\"source\":\"OTHER\","
                + "\"dateFound\":\"2026-11-01\",\"version\":" + version + extraFields + "}";
    }

    private long createCompany(String name) throws Exception {
        String body = mockMvc.perform(post("/api/companies").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long createApplication(long companyId, String extraField) throws Exception {
        String extra = extraField == null ? "" : "," + extraField;
        String body = mockMvc.perform(post("/api/applications").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyId\":" + companyId + ",\"position\":\"Java Developer\"" + extra + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}
