package com.example.job_tracker.document;

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
class DocumentApiTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void attachesAndDetachesDocumentsOnApplication() throws Exception {
        long applicationId = createApplication();
        long cvId = createDocument("CV_v1", "CV");
        long letterId = createDocument("Letter_Acme", "COVER_LETTER");

        mockMvc.perform(put("/api/applications/{a}/documents/{d}", applicationId, letterId))
                .andExpect(status().isNoContent());
        mockMvc.perform(put("/api/applications/{a}/documents/{d}", applicationId, cvId))
                .andExpect(status().isNoContent());
        // ponowne podpięcie jest idempotentne
        mockMvc.perform(put("/api/applications/{a}/documents/{d}", applicationId, cvId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/applications/{a}/documents", applicationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("CV"))
                .andExpect(jsonPath("$[1].type").value("COVER_LETTER"));

        mockMvc.perform(delete("/api/applications/{a}/documents/{d}", applicationId, cvId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/applications/{a}/documents", applicationId))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Letter_Acme"));
    }

    @Test
    void refusesToDeleteAttachedDocumentButDeletesUnusedOne() throws Exception {
        long applicationId = createApplication();
        long documentId = createDocument("CV_v1", "CV");

        mockMvc.perform(put("/api/applications/{a}/documents/{d}", applicationId, documentId))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/documents/{id}", documentId))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/applications/{a}/documents/{d}", applicationId, documentId))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/documents/{id}", documentId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/documents/{id}", documentId))
                .andExpect(status().isNotFound());
    }

    @Test
    void refusesToAttachArchivedDocument() throws Exception {
        long applicationId = createApplication();
        long documentId = createDocument("Old CV", "CV");

        mockMvc.perform(put("/api/documents/{id}", documentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Old CV\",\"type\":\"CV\",\"archived\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));

        mockMvc.perform(put("/api/applications/{a}/documents/{d}", applicationId, documentId))
                .andExpect(status().isConflict());
    }

    @Test
    void hidesArchivedDocumentsByDefaultAndFiltersByType() throws Exception {
        long active = createDocument("CV_active", "CV");
        long archived = createDocument("CV_old", "CV");
        createDocument("Letter", "COVER_LETTER");

        mockMvc.perform(put("/api/documents/{id}", archived)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"CV_old\",\"type\":\"CV\",\"archived\":true}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/documents"))
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/documents").param("includeArchived", "true"))
                .andExpect(jsonPath("$.length()").value(3));

        mockMvc.perform(get("/api/documents").param("type", "CV"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(active));
    }

    @Test
    void returnsValidationErrorsForInvalidDocument() throws Exception {
        mockMvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.type").exists());
    }

    @Test
    void returnsNotFoundForUnknownApplicationOrDocument() throws Exception {
        long documentId = createDocument("CV_v1", "CV");
        long applicationId = createApplication();

        mockMvc.perform(put("/api/applications/{a}/documents/{d}", 999_999, documentId))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/applications/{a}/documents/{d}", applicationId, 999_999))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/applications/{a}/documents", 999_999))
                .andExpect(status().isNotFound());
    }

    private long createDocument(String name, String type) throws Exception {
        String body = mockMvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"type\":\"" + type + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long createApplication() throws Exception {
        String companyBody = mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"DocCorp\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long companyId = ((Number) JsonPath.read(companyBody, "$.id")).longValue();

        String body = mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyId\":" + companyId + ",\"position\":\"Java Developer\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}
