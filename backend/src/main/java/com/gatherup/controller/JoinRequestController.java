package com.gatherup.controller;

import com.gatherup.domain.enums.JoinRequestStatus;
import com.gatherup.dto.request.ProcessJoinRequestRequest;
import com.gatherup.dto.request.SendJoinRequestRequest;
import com.gatherup.dto.response.ApiResponse;
import com.gatherup.dto.response.JoinRequestResponse;
import com.gatherup.dto.response.PageResponse;
import com.gatherup.service.JoinRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

@Tag(name = "Join Requests", description = "Send, process, and manage join requests")
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class JoinRequestController {

    private final JoinRequestService joinRequestService;

    @Operation(summary = "Send a join request for an event", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{eventId}/join")
    public ResponseEntity<ApiResponse<JoinRequestResponse>> sendJoinRequest(
            @PathVariable UUID eventId,
            @Valid @RequestBody(required = false) SendJoinRequestRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {

        String message = (req != null) ? req.message() : null;
        JoinRequestResponse response = joinRequestService.sendJoinRequest(eventId, userId(userDetails), message);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(summary = "Approve or reject a join request (creator only)", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{eventId}/join-requests/{requestId}")
    public ResponseEntity<ApiResponse<JoinRequestResponse>> processJoinRequest(
            @PathVariable UUID eventId,
            @PathVariable UUID requestId,
            @Valid @RequestBody ProcessJoinRequestRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(ApiResponse.success(
                joinRequestService.processJoinRequest(requestId, userId(userDetails), req.action())));
    }

    @Operation(summary = "Cancel your own join request", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{eventId}/join-requests/{requestId}")
    public ResponseEntity<ApiResponse<Void>> cancelJoinRequest(
            @PathVariable UUID eventId,
            @PathVariable UUID requestId,
            @AuthenticationPrincipal UserDetails userDetails) {

        joinRequestService.cancelJoinRequest(requestId, userId(userDetails));
        return ResponseEntity.ok(ApiResponse.success(null, "Join request cancelled"));
    }

    @Operation(summary = "List join requests for an event (creator only)", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{eventId}/join-requests")
    public ResponseEntity<ApiResponse<PageResponse<JoinRequestResponse>>> getJoinRequests(
            @PathVariable UUID eventId,
            @RequestParam(required = false) JoinRequestStatus status,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(ApiResponse.success(
                joinRequestService.getEventJoinRequests(eventId, userId(userDetails), status, pageable)));
    }

    private UUID userId(UserDetails ud) {
        return UUID.fromString(ud.getUsername());
    }
}
