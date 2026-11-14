package com.example.job_tracker.auth.dto;


import com.example.job_tracker.auth.TokenService;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {

    public static TokenResponse from(TokenService.AccessToken token) {
        return new TokenResponse(token.value(), "Bearer", token.expiresInSeconds());
    }
}
