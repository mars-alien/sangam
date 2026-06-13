package com.gatherup.controller;

import com.gatherup.dto.response.ApiResponse;
import com.gatherup.dto.response.GroupMemberResponse;
import com.gatherup.dto.response.PageResponse;
import com.gatherup.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Group Members", description = "View and manage event members")
@RestController
@RequestMapping("/api/v1/events/{eventId}")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @Operation(summary = "List active members of an event")
    @GetMapping("/members")
    public ResponseEntity<ApiResponse<PageResponse<GroupMemberResponse>>> getMembers(
            @PathVariable UUID eventId,
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.success(groupService.getEventMembers(eventId, pageable)));
    }

    @Operation(summary = "Leave an event", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/members/me")
    public ResponseEntity<ApiResponse<Void>> leaveEvent(
            @PathVariable UUID eventId,
            @AuthenticationPrincipal UserDetails userDetails) {

        groupService.leaveEvent(eventId, userId(userDetails));
        return ResponseEntity.ok(ApiResponse.success(null, "You have left the event"));
    }

    @Operation(summary = "Remove a member from an event (creator only)", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/members/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable UUID eventId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserDetails userDetails) {

        groupService.removeMember(eventId, userId, userId(userDetails));
        return ResponseEntity.ok(ApiResponse.success(null, "Member removed"));
    }

    private UUID userId(UserDetails ud) {
        return UUID.fromString(ud.getUsername());
    }
}
