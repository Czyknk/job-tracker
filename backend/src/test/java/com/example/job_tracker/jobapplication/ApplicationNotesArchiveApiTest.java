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

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class ApplicationNotesArchiveApiTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void notesAndArchivingWorkAndArchivedApplicationsAreHiddenByDefault() throws Exception {
        long companyId = createCompany("Archive Co");
        long id = createApplication(companyId);
        String company = String.valueOf(companyId);

        mockMvc.perform(put("/api/applications/{id}/notes", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"  Ask about the testing culture  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value("Ask about the testing culture"));

        mockMvc.perform(post("/api/applications/{id}/archive", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));

        mockMvc.perform(get("/api/applications").param("companyId", company))
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/applications").param("companyId", company).param("archived", "ARCHIVED"))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/applications").param("companyId", company).param("archived", "ALL"))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(post("/api/applications/{id}/unarchive", id))
                .andExpect(jsonPath("$.archived").value(false));
        mockMvc.perform(get("/api/applications").param("companyId", company))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void blankNotesClearThem() throws Exception {
        long id = createApplication(createCompany("Blank Notes Co"));

        mockMvc.perform(put("/api/applications/{id}/notes", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"something\"}"))
                .andExpect(jsonPath("$.notes").value("something"));
        mockMvc.perform(put("/api/applications/{id}/notes", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"   \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").doesNotExist());
    }

    @Test
    void unknownApplicationsAreNotFound() throws Exception {
        mockMvc.perform(post("/api/applications/{id}/archive", 999_999)).andExpect(status().isNotFound());
        mockMvc.perform(put("/api/applications/{id}/notes", 999_999).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"x\"}"))
                .andExpect(status().isNotFound());
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
