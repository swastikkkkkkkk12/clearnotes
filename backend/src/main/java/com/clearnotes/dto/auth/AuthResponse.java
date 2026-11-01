package com.clearnotes.dto.auth;

public record AuthResponse(String accessToken, String tokenType, long expiresInSeconds,
                           UserSummaryResponse user) { }
