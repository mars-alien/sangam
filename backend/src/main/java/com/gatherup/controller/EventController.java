package com.gatherup.controller;

import com.gatherup.dto.request.CreateEventRequest;
import com.gatherup.dto.request.UpdateEventRequest;
import com.gatherup.dto.response.ApiResponse;
import com.gatherup.dto.response.EventDetailResponse;
import com.gatherup.dto.response.EventSummaryResponse;
import com.gatherup.dto.response.PageResponse;
import com.gatherup.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Events", description = "Event discovery, creation, and management")
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @Operation(summary = "Browse nearby events by location radius")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<EventSummaryResponse>>> getNearbyEvents(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "10") double radiusKm,
            @RequestParam(required = false) String category,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication auth) {

        String categoryParam = (category == null || category.isBlank()) ? null : category;
        return ResponseEntity.ok(ApiResponse.success(
                eventService.getNearbyEvents(lat, lng, radiusKm, categoryParam, pageable)));
    }

    @Operation(summary = "Full-text search events by keyword")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<EventSummaryResponse>>> searchEvents(
            @RequestParam String q,
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.success(eventService.searchEvents(q, pageable)));
    }

    @Operation(summary = "Get event details")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventDetailResponse>> getEvent(
            @PathVariable UUID id,
            Authentication auth) {

        return ResponseEntity.ok(ApiResponse.success(
                eventService.getEvent(id, extractUserId(auth))));
    }

    @Operation(summary = "Create a new event", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    public ResponseEntity<ApiResponse<EventDetailResponse>> createEvent(
            @Valid @RequestBody CreateEventRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                eventService.createEvent(req, userId(userDetails)), "Event created successfully"));
    }

    @Operation(summary = "Update an event (creator only)", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EventDetailResponse>> updateEvent(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEventRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(ApiResponse.success(
                eventService.updateEvent(id, req, userId(userDetails))));
    }

    @Operation(summary = "Delete (soft-delete) an event (creator only)", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEvent(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {

        eventService.deleteEvent(id, userId(userDetails));
        return ResponseEntity.ok(ApiResponse.success(null, "Event deleted successfully"));
    }

    @Operation(summary = "Get events created by or joined by the current user", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<PageResponse<EventSummaryResponse>>> getMyEvents(
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(ApiResponse.success(
                eventService.getMyEvents(userId(userDetails), pageable)));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private UUID userId(UserDetails ud) {
        return UUID.fromString(ud.getUsername());
    }

    private UUID extractUserId(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof UserDetails ud)) {
            return null;
        }
        return UUID.fromString(ud.getUsername());
    }
}
