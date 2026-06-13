package com.gatherup.dto.response;

import java.time.Instant;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        Instant accessTokenExpiresAt,
        UserProfileResponse user
) {}
