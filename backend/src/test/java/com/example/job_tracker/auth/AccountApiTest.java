package com.example.job_tracker.auth;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.job_tracker.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * The owner account comes from src/test/resources/config/application.properties.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class AccountApiTest {

    private static final String EMAIL = "admin@example.com";
    private static final String PASSWORD = "test-admin-password";
    private static final String NEW_PASSWORD = "a-brand-new-password-1";

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void meReturnsTheLoggedInUser() throws Exception {
        String token = login(EMAIL, PASSWORD).accessToken();

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void meRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void changingThePasswordSwitchesCredentialsAndEndsOtherSessions() throws Exception {
        Login session = login(EMAIL, PASSWORD);

        mockMvc.perform(post("/api/auth/change-password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(PASSWORD, NEW_PASSWORD)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(credentials(EMAIL, PASSWORD)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(credentials(EMAIL, NEW_PASSWORD)))
                .andExpect(status().isOk());

        // the session that existed before the change can no longer be refreshed
        mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie("refresh_token", session.refreshToken())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidPasswordChangesAreBadRequests() throws Exception {
        String token = "Bearer " + login(EMAIL, PASSWORD).accessToken();

        mockMvc.perform(post("/api/auth/change-password").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(body("wrong-current-password", NEW_PASSWORD)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/auth/change-password").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(body(PASSWORD, "short")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/auth/change-password").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(body(PASSWORD, PASSWORD)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/auth/change-password").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.currentPassword").exists())
                .andExpect(jsonPath("$.errors.newPassword").exists());
    }

    private record Login(String accessToken, String refreshToken) {
    }

    private Login login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(credentials(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        String accessToken = JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
        return new Login(accessToken, result.getResponse().getCookie("refresh_token").getValue());
    }

    private static String credentials(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    private static String body(String current, String next) {
        return "{\"currentPassword\":\"" + current + "\",\"newPassword\":\"" + next + "\"}";
    }
}
