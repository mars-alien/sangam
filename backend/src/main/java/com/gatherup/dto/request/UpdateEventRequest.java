package com.gatherup.dto.request;

import com.gatherup.domain.enums.EventCategory;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Set;

public record UpdateEventRequest(
        @Size(max = 100) String title,
        @Size(max = 2000) String description,
        EventCategory category,
        Double latitude,
        Double longitude,
        String venueName,
        String address,
        String city,
        @Future Instant eventDate,
        Instant eventEndDate,
        @Min(1) Integer minCompanions,
        @Min(1) @Max(20) Integer maxCompanions,
        @Size(max = 5) Set<String> tags
) {}
