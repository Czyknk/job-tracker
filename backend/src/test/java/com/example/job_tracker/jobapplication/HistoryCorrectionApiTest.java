package com.example.job_tracker.jobapplication;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class HistoryCorrectionApiTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void theHistoryCanBeCorrected() throws Exception {
        long id = createApplication(createCompany("History Co"));
        changeStatus(id, "APPLIED");
        changeStatus(id, "HR_SCREEN");
        changeStatus(id, "REJECTED");
        mockMvc.perform(get("/api/applications/{id}", id)).andExpect(jsonPath("$.currentStatus").value("REJECTED"));

        String history = mockMvc.perform(get("/api/applications/{id}/history", id)).andReturn()
                .getResponse().getContentAsString();
        int rejectedEntry = JsonPath.read(history, "$[3].id");
        int hrEntry = JsonPath.read(history, "$[2].id");
        String hrChangedAt = JsonPath.read(history, "$[2].changedAt");

        // undo the rejection
        mockMvc.perform(delete("/api/applications/{id}/history/{entry}", id, rejectedEntry))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/applications/{id}", id)).andExpect(jsonPath("$.currentStatus").value("HR_SCREEN"));
        changeStatus(id, "TECH_INTERVIEW");

        // fix the note of an entry
        mockMvc.perform(put("/api/applications/{id}/history/{entry}", id, hrEntry)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"HR_SCREEN\",\"changedAt\":\"" + hrChangedAt + "\",\"note\":\"Corrected note\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.note").value("Corrected note"));
    }

    @Test
    void aFinalStatusCannotBeMovedBeforeOtherEntriesAndTheLastEntryCannotBeDeleted() throws Exception {
        long id = createApplication(createCompany("Rules Co"));
        String history = mockMvc.perform(get("/api/applications/{id}/history", id)).andReturn()
                .getResponse().getContentAsString();
        int foundEntry = JsonPath.read(history, "$[0].id");
        String foundAt = JsonPath.read(history, "$[0].changedAt");

        mockMvc.perform(delete("/api/applications/{id}/history/{entry}", id, foundEntry))
                .andExpect(status().isConflict());

        changeStatus(id, "REJECTED");
        // turning the first entry into a final status would leave a final status in the middle
        mockMvc.perform(put("/api/applications/{id}/history/{entry}", id, foundEntry)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"WITHDRAWN\",\"changedAt\":\"" + foundAt + "\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void unknownEntriesAreNotFound() throws Exception {
        long id = createApplication(createCompany("Unknown Entry Co"));

        mockMvc.perform(delete("/api/applications/{id}/history/{entry}", id, 999_999))
                .andExpect(status().isNotFound());
    }

    private void changeStatus(long applicationId, String status) throws Exception {
        mockMvc.perform(post("/api/applications/{id}/status", applicationId)
                        .contentType(MediaType.APPLICATION_JSON)
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

    private long createApplication(long companyId) throws Exception {
        String body = mockMvc.perform(post("/api/applications").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyId\":" + companyId + ",\"position\":\"Java Developer\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}
