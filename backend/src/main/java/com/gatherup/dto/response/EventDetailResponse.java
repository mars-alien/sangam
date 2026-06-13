package com.gatherup.dto.response;

import com.gatherup.domain.enums.EventCategory;
import com.gatherup.domain.enums.EventStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class EventDetailResponse {

    // ── Shared with EventSummaryResponse ─────────────────────────────────────
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
    @Setter private Double distanceKm;
    private boolean full;
    private Set<String> tags;
    private Instant createdAt;

    // ── Detail-only fields ───────────────────────────────────────────────────
    private String description;
    private String address;
    private Double latitude;
    private Double longitude;
    private Instant eventEndDate;
    private int minCompanions;
    private String imageUrl;

    // Computed by service against the authenticated user.
    // @JsonProperty forces JSON key to "isCreator" — without it Jackson strips "is" prefix and
    // the field collides with the UserSummaryResponse "creator" field above.
    @JsonProperty("isCreator")
    private boolean isCreator;
    @Setter private String currentUserStatus;   // NONE | PENDING | APPROVED | WAITLISTED | REJECTED
    @Setter private Integer waitlistPosition;

    public void setIsCreator(boolean isCreator) { this.isCreator = isCreator; }
}
