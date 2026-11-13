package com.example.job_tracker.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AppUserRepository users;
    private final TokenService tokens;

    public AuthController(AuthenticationManager authenticationManager, AppUserRepository users, TokenService tokens) {
        this.authenticationManager = authenticationManager;
        this.users = users;
        this.tokens = tokens;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));
        AppUser user = users.findByEmailIgnoreCase(request.email()).orElseThrow();
        return new LoginResponse(tokens.issue(user), "Bearer");
    }

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}

    public record LoginResponse(String accessToken, String tokenType) {}
}
