package com.example.job_tracker.auth;

import com.example.job_tracker.auth.dto.LoginRequest;
import com.example.job_tracker.auth.dto.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    @PostMapping("/login")
    @SecurityRequirements // endpoint publiczny: bez kłódki w Swagger UI
    @Operation(summary = "Log in with e-mail and password and receive a short-lived JWT access token",
            description = "Use the returned accessToken in the Authorize dialog (paste the token only, "
                    + "without the 'Bearer ' prefix) or send it as 'Authorization: Bearer <token>'.")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return TokenResponse.from(authService.login(request.email(), request.password()));
    }
}
