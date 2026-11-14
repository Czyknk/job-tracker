package com.example.job_tracker.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

/**
 * Building and reading the cookie with the refresh token. The cookie is httpOnly (invisible to JavaScript),
 * SameSite=Strict, and limited to the path /api/auth, so the browser sends it only for login,
 * refresh, and logout.
 */
@Component
@RequiredArgsConstructor
public class RefreshCookies {

    private static final String PATH = "/api/auth";

    private final RefreshProperties properties;

    public ResponseCookie create(RefreshTokenService.IssuedRefreshToken token) {
        return base(token.value()).maxAge(token.ttl()).build();
    }

    public ResponseCookie clear() {
        return base("").maxAge(Duration.ZERO).build();
    }

    public Optional<String> read(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, properties.cookieName());
        return Optional.ofNullable(cookie).map(Cookie::getValue);
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(properties.cookieName(), value)
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite("Strict")
                .path(PATH);
    }
}
