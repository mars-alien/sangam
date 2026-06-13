package com.gatherup.event;

import java.util.UUID;

public record MemberLeftEvent(UUID eventId, UUID userId) {}
