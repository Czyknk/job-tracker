package com.example.job_tracker.auth;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
 * Full session flow: login, refresh token rotation, reuse detection, and logout on a real database.
 * The admin account comes from src/test/resources/config/application.properties.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class AuthFlowApiTest {

    private static final String COOKIE = "refresh_token";
    private static final String ADMIN_LOGIN =
            "{\"email\":\"admin@example.com\",\"password\":\"test-admin-password\"}";

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void loginSetsAnHttpOnlyScopedRefreshCookie() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(ADMIN_LOGIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(cookie().httpOnly(COOKIE, true))
                .andExpect(cookie().secure(COOKIE, true))
                .andExpect(cookie().path(COOKIE, "/api/auth"))
                .andExpect(cookie().maxAge(COOKIE, 7 * 24 * 3600))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Strict")));
    }

    @Test
    void refreshRotatesTheCookieAndIssuesAWorkingAccessToken() throws Exception {
        String first = loginAndGetRefreshCookie();

        MvcResult refreshed = mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie(COOKIE, first)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        String second = refreshed.getResponse().getCookie(COOKIE).getValue();
        org.assertj.core.api.Assertions.assertThat(second).isNotBlank().isNotEqualTo(first);

        String accessToken = JsonPath.read(refreshed.getResponse().getContentAsString(), "$.accessToken");
        mockMvc.perform(get("/api/companies").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    void reusingAConsumedRefreshTokenRevokesTheWholeSession() throws Exception {
        String first = loginAndGetRefreshCookie();
        MvcResult refreshed = mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie(COOKIE, first)))
                .andExpect(status().isOk())
                .andReturn();
        String second = refreshed.getResponse().getCookie(COOKIE).getValue();

        // someone uses the old (already consumed) token
        mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie(COOKIE, first)))
                .andExpect(status().isUnauthorized());

        // the new token is also revoked, so the session is gone
        mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie(COOKIE, second)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesTheSessionAndClearsTheCookie() throws Exception {
        String refreshToken = loginAndGetRefreshCookie();

        mockMvc.perform(post("/api/auth/logout").cookie(new Cookie(COOKIE, refreshToken)))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge(COOKIE, 0));

        mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie(COOKIE, refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sessionsAreIndependentOfEachOther() throws Exception {
        String firstSession = loginAndGetRefreshCookie();
        String secondSession = loginAndGetRefreshCookie();

        mockMvc.perform(post("/api/auth/logout").cookie(new Cookie(COOKIE, firstSession)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie(COOKIE, secondSession)))
                .andExpect(status().isOk());
    }

    @Test
    void refreshWithoutOrWithGarbageCookieIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie(COOKIE, "garbage")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutWithoutACookieIsHarmless() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, not(containsString("garbage"))));
    }

    private String loginAndGetRefreshCookie() throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(ADMIN_LOGIN))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getCookie(COOKIE).getValue();
    }
}
