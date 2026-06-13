package com.gatherup.event;

import java.util.UUID;

public record JoinRequestApprovedEvent(UUID eventId, UUID requesterId, UUID approverId, boolean promotedFromWaitlist) {}
