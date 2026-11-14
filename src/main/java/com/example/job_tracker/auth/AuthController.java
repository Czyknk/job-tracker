package com.example.job_tracker.auth;

import com.example.job_tracker.auth.dto.LoginRequest;
import com.example.job_tracker.auth.dto.TokenResponse;
import com.example.job_tracker.common.exceptions.UnauthorizedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;
    private final RefreshCookies refreshCookies;

    @PostMapping("/login")
    @SecurityRequirements // No authentication required for login
    @Operation(summary = "Log in with e-mail and password",
            description = "Returns a short-lived JWT access token in the body and starts a session by setting "
                    + "an httpOnly refresh-token cookie. Use the accessToken in the Authorize dialog "
                    + "(paste the token only, without the 'Bearer ' prefix).")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return respond(authService.login(request.email(), request.password()));
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Get a new access token using the refresh-token cookie",
            description = "Rotates the refresh token: the old one is consumed and a new cookie is set. "
                    + "Reusing an already consumed token revokes the whole session (401).")
    public ResponseEntity<TokenResponse> refresh(HttpServletRequest request) {
        String refreshToken = refreshCookies.read(request)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        return respond(authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    @SecurityRequirements
    @Operation(summary = "End the session: revoke the refresh token and clear the cookie (always 204)")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        authService.logout(refreshCookies.read(request).orElse(null));
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookies.clear().toString())
                .build();
    }

    private ResponseEntity<TokenResponse> respond(AuthService.Session session) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookies.create(session.refreshToken()).toString())
                .body(TokenResponse.from(session.accessToken()));
    }
}
