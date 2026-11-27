package com.example.job_tracker.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

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
 * GET /api/companies/lookup: lean autocomplete of companies by name.
 * Names use unusual words so that companies created by other tests in the same database cannot match.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class CompanyLookupApiTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void matchesAnywhereIgnoringCaseWithPrefixMatchesFirst() throws Exception {
        createCompany("Zeta Quokka Labs");
        createCompany("Quokka Soft");
        createCompany("Blue Quokka");
        createCompany("Wombat Works");

        mockMvc.perform(get("/api/companies/lookup").param("q", "  QUOKKA "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name").value(contains("Quokka Soft", "Blue Quokka", "Zeta Quokka Labs")));
    }

    @Test
    void itemsAreLean() throws Exception {
        long id = createCompany("Lean Numbat Ltd");

        mockMvc.perform(get("/api/companies/lookup").param("q", "numbat"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value((int) id))
                .andExpect(jsonPath("$[0].name").value("Lean Numbat Ltd"))
                .andExpect(jsonPath("$[0].notes").doesNotExist())
                .andExpect(jsonPath("$[0].website").doesNotExist())
                .andExpect(jsonPath("$[0].createdAt").doesNotExist());
    }

    @Test
    void theLimitHasADefaultAndIsClamped() throws Exception {
        for (int i = 1; i <= 22; i++) {
            createCompany(String.format("Axolotl %02d", i));
        }

        mockMvc.perform(get("/api/companies/lookup").param("q", "axolotl"))
                .andExpect(jsonPath("$", hasSize(CompanyService.LOOKUP_DEFAULT_LIMIT)))
                .andExpect(jsonPath("$[0].name").value("Axolotl 01"));
        mockMvc.perform(get("/api/companies/lookup").param("q", "axolotl").param("limit", "100"))
                .andExpect(jsonPath("$", hasSize(CompanyService.LOOKUP_MAX_LIMIT)));
        mockMvc.perform(get("/api/companies/lookup").param("q", "axolotl").param("limit", "0"))
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/api/companies/lookup").param("q", "axolotl").param("limit", "3"))
                .andExpect(jsonPath("$[*].name").value(contains("Axolotl 01", "Axolotl 02", "Axolotl 03")));
    }

    @Test
    void aBlankQueryReturnsTheFirstCompaniesByName() throws Exception {
        createCompany("Aardvark First");
        createCompany("Zebra Last");

        String body = mockMvc.perform(get("/api/companies/lookup").param("q", " "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(lessThanOrEqualTo(CompanyService.LOOKUP_DEFAULT_LIMIT))))
                .andReturn().getResponse().getContentAsString();

        // The exact order follows the database collation, so only check that the alphabetically first one is there
        List<String> names = JsonPath.read(body, "$[*].name");
        assertThat(names).isNotEmpty().contains("Aardvark First");
        mockMvc.perform(get("/api/companies/lookup")).andExpect(status().isOk());
    }

    @Test
    void wildcardCharactersMatchLiterally() throws Exception {
        createCompany("Gecko 100% Remote");
        createCompany("Gecko Under_score");
        createCompany("Gecko Underscore");
        createCompany("Gecko Bang! Co");

        mockMvc.perform(get("/api/companies/lookup").param("q", "%"))
                .andExpect(jsonPath("$[*].name").value(contains("Gecko 100% Remote")));
        mockMvc.perform(get("/api/companies/lookup").param("q", "r_s"))
                .andExpect(jsonPath("$[*].name").value(contains("Gecko Under_score")));
        mockMvc.perform(get("/api/companies/lookup").param("q", "bang!"))
                .andExpect(jsonPath("$[*].name").value(contains("Gecko Bang! Co")));
    }

    private long createCompany(String name) throws Exception {
        String body = mockMvc.perform(post("/api/companies").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}
