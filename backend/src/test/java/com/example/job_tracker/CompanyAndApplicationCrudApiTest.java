package com.example.job_tracker;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
 * Testy edycji i usuwania firm oraz aplikacji. Przed DELETE czyścimy kontekst persystencji,
 * bo kaskady (ON DELETE CASCADE) działają w bazie, a nie w Hibernate.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class CompanyAndApplicationCrudApiTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private EntityManager entityManager;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    // ----------------------------------------------------------- companies
    @Test
    void updatesCompanyAndRejectsNameOfAnotherCompany() throws Exception {
        long first = createCompany("First Co");
        createCompany("Second Co");

        mockMvc.perform(put("/api/companies/{id}", first)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"FIRST CO\",\"industry\":\"Fintech\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("FIRST CO"))
                .andExpect(jsonPath("$.industry").value("Fintech"));

        mockMvc.perform(put("/api/companies/{id}", first)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"second co\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/companies/{id}", 999_999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Whatever\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void refusesToDeleteCompanyWithApplicationsButDeletesEmptyOneWithItsContacts() throws Exception {
        long busyCompany = createCompany("Busy Co");
        createApplication(busyCompany);

        mockMvc.perform(delete("/api/companies/{id}", busyCompany))
                .andExpect(status().isConflict());

        long emptyCompany = createCompany("Empty Co");
        long contactId = createContact(emptyCompany);
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(delete("/api/companies/{id}", emptyCompany))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/companies/{id}", emptyCompany))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/contacts/{id}", contactId))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------- applications
    @Test
    void updatesApplicationFieldsMovesItToAnotherCompanyAndClearsSalary() throws Exception {
        long companyId = createCompany("Crud Co");
        long otherCompanyId = createCompany("Other Co");
        long applicationId = createApplication(companyId);

        mockMvc.perform(put("/api/applications/{id}", applicationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyId\":" + otherCompanyId + ",\"position\":\"Senior Java Developer\","
                                + "\"source\":\"LINKEDIN\",\"dateFound\":\"2026-10-01\","
                                + "\"location\":\"Remote\",\"workMode\":\"REMOTE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value("Senior Java Developer"))
                .andExpect(jsonPath("$.companyName").value("Other Co"))
                .andExpect(jsonPath("$.source").value("LINKEDIN"))
                .andExpect(jsonPath("$.dateFound").value("2026-10-01"))
                .andExpect(jsonPath("$.salary").doesNotExist());

        mockMvc.perform(get("/api/applications/{id}", applicationId))
                .andExpect(jsonPath("$.companyId").value(otherCompanyId))
                .andExpect(jsonPath("$.workMode").value("REMOTE"))
                .andExpect(jsonPath("$.currentStatus").value("FOUND"));
    }

    @Test
    void applicationUpdateValidatesAndReportsMissingResources() throws Exception {
        long applicationId = createApplication(createCompany("Valid Co"));

        mockMvc.perform(put("/api/applications/{id}", applicationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"position\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.companyId").exists())
                .andExpect(jsonPath("$.errors.position").exists())
                .andExpect(jsonPath("$.errors.source").exists())
                .andExpect(jsonPath("$.errors.dateFound").exists());

        mockMvc.perform(put("/api/applications/{id}", applicationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyId\":999999,\"position\":\"Dev\",\"source\":\"OTHER\","
                                + "\"dateFound\":\"2026-10-01\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/applications/{id}", 999_999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyId\":" + 1 + ",\"position\":\"Dev\",\"source\":\"OTHER\","
                                + "\"dateFound\":\"2026-10-01\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletesApplicationWithHistoryAndKeepsAttachedDocument() throws Exception {
        long companyId = createCompany("Delete Co");
        long applicationId = createApplication(companyId);
        long documentId = createDocument();

        mockMvc.perform(post("/api/applications/{id}/status", applicationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPLIED\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(put("/api/applications/{a}/documents/{d}", applicationId, documentId))
                .andExpect(status().isNoContent());

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(delete("/api/applications/{id}", applicationId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/applications/{id}", applicationId))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/applications/{id}/history", applicationId))
                .andExpect(status().isNotFound());

        // dokument zostaje i po odpięciu od usuniętej aplikacji można go już usunąć
        mockMvc.perform(get("/api/documents/{id}", documentId))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/documents/{id}", documentId))
                .andExpect(status().isNoContent());

        // firma bez aplikacji da się teraz usunąć
        mockMvc.perform(delete("/api/companies/{id}", companyId))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteReturnsNotFoundForUnknownApplication() throws Exception {
        mockMvc.perform(delete("/api/applications/{id}", 999_999))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------- helpers
    private long createCompany(String name) throws Exception {
        return idOf(mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    private long createContact(long companyId) throws Exception {
        return idOf(mockMvc.perform(post("/api/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyId\":" + companyId + ",\"firstName\":\"Anna\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    private long createDocument() throws Exception {
        return idOf(mockMvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"CV_v1\",\"type\":\"CV\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    private long createApplication(long companyId) throws Exception {
        return idOf(mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyId\":" + companyId + ",\"position\":\"Java Developer\","
                                + "\"source\":\"PRACUJ_PL\",\"salary\":{\"min\":8000,\"max\":12000,"
                                + "\"currency\":\"PLN\",\"period\":\"MONTH\",\"basis\":\"GROSS\"}}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    private static long idOf(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }
}
