package com.gatherup.dto.response;

import com.gatherup.domain.enums.MemberRole;

import java.time.Instant;
import java.util.UUID;

public record GroupMemberResponse(
        UUID id,
        UserSummaryResponse user,
        MemberRole role,
        Instant joinedAt
) {}
