package com.example.job_tracker.auth;

import com.example.job_tracker.auth.dto.ChangePasswordRequest;
import com.example.job_tracker.auth.dto.CurrentUserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Account")
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/me")
    @Operation(summary = "Get the logged-in user (id, e-mail, role)")
    public CurrentUserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return CurrentUserResponse.from(accountService.currentUser(userId(jwt)));
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Change the password of the logged-in user",
            description = "All sessions are ended (refresh tokens revoked); the current access token works "
                    + "until it expires. A wrong current password or a weak new password is a 400.")
    public void changePassword(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChangePasswordRequest request) {
        accountService.changePassword(userId(jwt), request.currentPassword(), request.newPassword());
    }

    // The token subject is the user id (see TokenService).
    private static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
