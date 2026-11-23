package com.example.job_tracker.auth.dto;

import com.example.job_tracker.auth.AppUser;
import com.example.job_tracker.auth.Role;

public record CurrentUserResponse(Long id, String email, Role role) {

    public static CurrentUserResponse from(AppUser user) {
        return new CurrentUserResponse(user.getId(), user.getEmail(), user.getRole());
    }
}
