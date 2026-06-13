package com.gatherup.dto.request;

import com.gatherup.domain.enums.EventCategory;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Set;

public record CreateEventRequest(
        @NotBlank @Size(max = 100) String title,
        @NotBlank @Size(max = 2000) String description,
        @NotNull EventCategory category,
        @NotNull Double latitude,
        @NotNull Double longitude,
        @NotBlank String venueName,
        String address,
        @NotBlank String city,
        @NotNull @Future Instant eventDate,
        Instant eventEndDate,
        @Min(1) int minCompanions,
        @Min(1) @Max(20) int maxCompanions,
        @Size(max = 5) Set<String> tags
) {}
