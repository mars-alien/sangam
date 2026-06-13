package com.gatherup.dto.response;

import com.gatherup.domain.enums.JoinRequestStatus;

import java.time.Instant;
import java.util.UUID;

public record JoinRequestResponse(
        UUID id,
        EventSummaryResponse event,
        UserSummaryResponse requester,
        JoinRequestStatus status,
        String message,
        Integer waitlistPosition,
        Instant createdAt,
        Instant respondedAt
) {}
