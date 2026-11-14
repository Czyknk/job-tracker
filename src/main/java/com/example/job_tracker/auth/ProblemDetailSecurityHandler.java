package com.example.job_tracker.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Handles security-related errors (401 Unauthorized and 403 Forbidden) and returns them in a standardized Problem Detail format (RFC 7807).
 * This ensures that all security errors are consistent with the rest of the API's error handling.
 */
@Component
public class ProblemDetailSecurityHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(@NonNull HttpServletRequest request, HttpServletResponse response,
                         @NonNull AuthenticationException authException) throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        write(response, 401, "Unauthorized", "Authentication is required to access this resource");
    }

    @Override
    public void handle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                       @NonNull AccessDeniedException accessDeniedException) throws IOException {
        write(response, 403, "Forbidden", "You do not have permission to access this resource");
    }

    private static void write(HttpServletResponse response, int status, String title, String detail)
            throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/problem+json");
        response.getWriter().write(String.format(
                "{\"type\":\"about:blank\",\"title\":\"%s\",\"status\":%d,\"detail\":\"%s\"}",
                title, status, detail));
    }
}
