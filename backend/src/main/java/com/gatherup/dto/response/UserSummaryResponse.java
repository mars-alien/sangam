package com.gatherup.dto.response;

import java.util.UUID;

public record UserSummaryResponse(
        UUID id,
        String username,
        String avatarUrl,
        String city
) {}
