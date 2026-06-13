package com.gatherup.dto.response;

import com.gatherup.domain.enums.EventCategory;
import com.gatherup.domain.enums.EventStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventSummaryResponse {

    private UUID id;
    private String title;
    private EventStatus status;
    private EventCategory category;
    private String venueName;
    private String city;
    private Instant eventDate;
    private int currentMemberCount;
    private int maxCompanions;
    private UserSummaryResponse creator;

    // Populated by geospatial query in service — null for non-proximity searches
    @Setter
    private Double distanceKm;

    // Coordinates for map rendering
    @Setter
    private Double latitude;
    @Setter
    private Double longitude;

    // Computed by mapper: currentMemberCount >= maxCompanions
    private boolean full;

    private Set<String> tags;
    private Instant createdAt;
}
