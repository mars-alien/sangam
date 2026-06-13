package com.gatherup.event;

import java.util.List;
import java.util.UUID;

public record EventCancelledEvent(UUID eventId, List<UUID> memberIds) {}
