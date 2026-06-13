package com.gatherup.event;

import java.util.UUID;

public record JoinRequestRejectedEvent(UUID eventId, UUID requesterId) {}
