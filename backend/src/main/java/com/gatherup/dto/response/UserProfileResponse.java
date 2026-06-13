package com.gatherup.dto.response;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String username,
        String email,
        String avatarUrl,
        String bio,
        String city,
        int totalEventsCreated,
        Instant createdAt
) {}
