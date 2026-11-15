package com.example.job_tracker.contact;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class ContactApiTest {

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
    void contactCrudFlow() throws Exception {
        long companyId = createCompany("ContactCorp");
        long contactId = createContact(companyId, "Anna");

        mockMvc.perform(get("/api/contacts/{id}", contactId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Anna"))
                .andExpect(jsonPath("$.companyName").value("ContactCorp"));

        mockMvc.perform(get("/api/contacts").param("companyId", String.valueOf(companyId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(put("/api/contacts/{id}", contactId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyId\":" + companyId + ",\"firstName\":\"Anna\","
                                + "\"lastName\":\"Nowak\",\"position\":\"Tech Recruiter\","
                                + "\"email\":\"anna@contactcorp.example\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Nowak"))
                .andExpect(jsonPath("$.position").value("Tech Recruiter"));

        mockMvc.perform(delete("/api/contacts/{id}", contactId))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/contacts/{id}", contactId))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsValidationErrorsForInvalidContact() throws Exception {
        mockMvc.perform(post("/api/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"\",\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.companyId").exists())
                .andExpect(jsonPath("$.errors.firstName").exists())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    void createReturnsNotFoundForUnknownCompany() throws Exception {
        mockMvc.perform(post("/api/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyId\":999999,\"firstName\":\"Anna\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void statusChangeCanCarryContactsAndEntryContactsCanBeEdited() throws Exception {
        long companyId = createCompany("InterviewCorp");
        long applicationId = createApplication(companyId);
        long anna = createContact(companyId, "Anna");
        long piotr = createContact(companyId, "Piotr");

        String entryBody = mockMvc.perform(post("/api/applications/{id}/status", applicationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"TECH_INTERVIEW\",\"contactIds\":[" + anna + "," + piotr + "]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contacts.length()").value(2))
                .andReturn().getResponse().getContentAsString();
        long entryId = ((Number) JsonPath.read(entryBody, "$.id")).longValue();

        mockMvc.perform(get("/api/applications/{id}/history", applicationId))
                .andExpect(jsonPath("$[1].status").value("TECH_INTERVIEW"))
                .andExpect(jsonPath("$[1].contacts.length()").value(2))
                .andExpect(jsonPath("$[0].contacts.length()").value(0));

        mockMvc.perform(delete("/api/applications/{a}/history/{e}/contacts/{c}", applicationId, entryId, piotr))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/applications/{id}/history", applicationId))
                .andExpect(jsonPath("$[1].contacts.length()").value(1))
                .andExpect(jsonPath("$[1].contacts[0].firstName").value("Anna"));

        // ponowne dodanie jest idempotentne
        mockMvc.perform(put("/api/applications/{a}/history/{e}/contacts/{c}", applicationId, entryId, piotr))
                .andExpect(status().isNoContent());
        mockMvc.perform(put("/api/applications/{a}/history/{e}/contacts/{c}", applicationId, entryId, piotr))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/applications/{id}/history", applicationId))
                .andExpect(jsonPath("$[1].contacts.length()").value(2));
    }

    @Test
    void unknownContactInStatusChangeLeavesApplicationUntouched() throws Exception {
        long applicationId = createApplication(createCompany("UntouchedCorp"));

        mockMvc.perform(post("/api/applications/{id}/status", applicationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"HR_SCREEN\",\"contactIds\":[999999]}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/applications/{id}", applicationId))
                .andExpect(jsonPath("$.currentStatus").value("FOUND"));
    }

    @Test
    void entryOfAnotherApplicationIsNotAccessible() throws Exception {
        long companyId = createCompany("MixCorp");
        long applicationA = createApplication(companyId);
        long applicationB = createApplication(companyId);
        long contactId = createContact(companyId, "Anna");

        String entryBody = mockMvc.perform(post("/api/applications/{id}/status", applicationA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPLIED\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long entryOfA = ((Number) JsonPath.read(entryBody, "$.id")).longValue();

        mockMvc.perform(put("/api/applications/{a}/history/{e}/contacts/{c}", applicationB, entryOfA, contactId))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletingContactRemovesItFromHistoryEntries() throws Exception {
        long companyId = createCompany("CascadeCorp");
        long applicationId = createApplication(companyId);
        long contactId = createContact(companyId, "Anna");

        mockMvc.perform(post("/api/applications/{id}/status", applicationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"HR_SCREEN\",\"contactIds\":[" + contactId + "]}"))
                .andExpect(status().isCreated());

        // zapisujemy zmiany i czyścimy kontekst, żeby usunięcie szło prosto do bazy (ON DELETE CASCADE)
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(delete("/api/contacts/{id}", contactId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/applications/{id}/history", applicationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].contacts.length()").value(0));
    }

    private long createCompany(String name) throws Exception {
        String body = mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long createContact(long companyId, String firstName) throws Exception {
        String body = mockMvc.perform(post("/api/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyId\":" + companyId + ",\"firstName\":\"" + firstName + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long createApplication(long companyId) throws Exception {
        String body = mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyId\":" + companyId + ",\"position\":\"Java Developer\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}
