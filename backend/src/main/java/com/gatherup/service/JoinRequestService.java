package com.gatherup.service;

import com.gatherup.domain.Event;
import com.gatherup.domain.GroupMember;
import com.gatherup.domain.JoinRequest;
import com.gatherup.domain.enums.EventStatus;
import com.gatherup.domain.enums.JoinRequestStatus;
import com.gatherup.domain.enums.MemberRole;
import com.gatherup.domain.enums.MemberStatus;
import com.gatherup.dto.response.JoinRequestResponse;
import com.gatherup.dto.response.PageResponse;
import com.gatherup.event.JoinRequestApprovedEvent;
import com.gatherup.event.JoinRequestRejectedEvent;
import com.gatherup.exception.DuplicateResourceException;
import com.gatherup.exception.InvalidOperationException;
import com.gatherup.exception.ResourceNotFoundException;
import com.gatherup.exception.UnauthorizedException;
import com.gatherup.mapper.JoinRequestMapper;
import com.gatherup.repository.EventRepository;
import com.gatherup.repository.GroupMemberRepository;
import com.gatherup.repository.JoinRequestRepository;
import com.gatherup.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JoinRequestService {

    private static final Set<EventStatus> JOINABLE = EnumSet.of(EventStatus.OPEN, EventStatus.FULL);
    private static final Set<JoinRequestStatus> ACTIVE_REQUEST_STATUSES =
            EnumSet.of(JoinRequestStatus.PENDING, JoinRequestStatus.APPROVED, JoinRequestStatus.WAITLISTED);

    private final JoinRequestRepository joinRequestRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final JoinRequestMapper joinRequestMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public JoinRequestResponse sendJoinRequest(UUID eventId, UUID requesterId, String message) {
        Event event = eventRepository.findByIdWithDetails(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));

        if (!JOINABLE.contains(event.getStatus())) {
            throw new InvalidOperationException("Cannot join a " + event.getStatus() + " event");
        }
        if (event.getCreator().getId().equals(requesterId)) {
            throw new InvalidOperationException("You are the creator of this event");
        }

        joinRequestRepository.findByEventIdAndRequesterId(eventId, requesterId)
                .ifPresent(jr -> {
                    if (ACTIVE_REQUEST_STATUSES.contains(jr.getStatus())) {
                        throw new DuplicateResourceException("You have already requested to join this event");
                    }
                });

        if (groupMemberRepository.existsByEventIdAndUserIdAndStatus(eventId, requesterId, MemberStatus.ACTIVE)) {
            throw new DuplicateResourceException("You are already a member of this event");
        }

        var requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("User", requesterId));

        JoinRequest jr = new JoinRequest();
        jr.setEvent(event);
        jr.setRequester(requester);
        jr.setMessage(message);

        if (event.getStatus() == EventStatus.FULL) {
            int nextPos = joinRequestRepository.findMaxWaitlistPositionByEventId(eventId)
                    .map(max -> max + 1)
                    .orElse(1);
            jr.setStatus(JoinRequestStatus.WAITLISTED);
            jr.setWaitlistPosition(nextPos);
            jr = joinRequestRepository.save(jr);
            log.info("User {} added to waitlist at position {} for event {}", requesterId, nextPos, eventId);
        } else {
            jr.setStatus(JoinRequestStatus.PENDING);
            jr = joinRequestRepository.save(jr);
            log.info("Join request created for event {} by user {}", eventId, requesterId);
        }

        return joinRequestMapper.toResponse(jr);
    }

    @Transactional
    public JoinRequestResponse processJoinRequest(UUID requestId, UUID approverId, String action) {
        JoinRequest jr = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("JoinRequest", requestId));

        Event event = jr.getEvent();
        if (!event.getCreator().getId().equals(approverId)) {
            throw new UnauthorizedException("Only the event creator can process join requests");
        }
        if (jr.getStatus() != JoinRequestStatus.PENDING) {
            throw new InvalidOperationException("Request is already " + jr.getStatus() + " — cannot process again");
        }

        jr.setRespondedAt(Instant.now());

        if ("APPROVE".equals(action)) {
            jr.setStatus(JoinRequestStatus.APPROVED);

            GroupMember member = new GroupMember();
            member.setEvent(event);
            member.setUser(jr.getRequester());
            member.setRole(MemberRole.MEMBER);
            member.setStatus(MemberStatus.ACTIVE);
            groupMemberRepository.save(member);

            event.setCurrentMemberCount(event.getCurrentMemberCount() + 1);
            if (event.getCurrentMemberCount() >= event.getMaxCompanions()) {
                event.setStatus(EventStatus.FULL);
            }
            eventRepository.save(event);

            log.info("Join request {} approved for event {}", requestId, event.getId());
            eventPublisher.publishEvent(new JoinRequestApprovedEvent(
                    event.getId(), jr.getRequester().getId(), approverId, false));

        } else {
            jr.setStatus(JoinRequestStatus.REJECTED);
            log.info("Join request {} rejected for event {}", requestId, event.getId());
            eventPublisher.publishEvent(new JoinRequestRejectedEvent(event.getId(), jr.getRequester().getId()));
        }

        return joinRequestMapper.toResponse(joinRequestRepository.save(jr));
    }

    @Transactional
    public void cancelJoinRequest(UUID requestId, UUID requesterId) {
        JoinRequest jr = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("JoinRequest", requestId));

        if (!jr.getRequester().getId().equals(requesterId)) {
            throw new UnauthorizedException("You can only cancel your own join requests");
        }
        if (jr.getStatus() != JoinRequestStatus.PENDING && jr.getStatus() != JoinRequestStatus.WAITLISTED) {
            throw new InvalidOperationException("Cannot cancel a " + jr.getStatus() + " request");
        }

        boolean wasWaitlisted = jr.getStatus() == JoinRequestStatus.WAITLISTED;
        jr.setStatus(JoinRequestStatus.CANCELLED);
        joinRequestRepository.save(jr);

        if (wasWaitlisted) {
            reindexWaitlist(jr.getEvent().getId());
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<JoinRequestResponse> getEventJoinRequests(
            UUID eventId, UUID requestingUserId, JoinRequestStatus status, Pageable pageable) {

        Event event = eventRepository.findByIdWithDetails(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));

        if (!event.getCreator().getId().equals(requestingUserId)) {
            throw new UnauthorizedException("Only the event creator can view join requests");
        }

        Page<JoinRequest> page = (status != null)
                ? joinRequestRepository.findByEventIdAndStatus(eventId, status, pageable)
                : joinRequestRepository.findByEventId(eventId, pageable);

        Page<JoinRequestResponse> mapped = page.map(joinRequestMapper::toResponse);
        return PageResponse.from(mapped);
    }

    // Called by GroupService.leaveEvent and EventLifecycleScheduler
    @Transactional
    public void promoteFromWaitlist(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));

        if (!JOINABLE.contains(event.getStatus())) {
            return;
        }

        int available = event.getMaxCompanions() - event.getCurrentMemberCount();
        if (available <= 0) return;

        List<JoinRequest> waitlisted = joinRequestRepository
                .findByEventIdAndStatusOrderByWaitlistPositionAsc(eventId, JoinRequestStatus.WAITLISTED);

        if (waitlisted.isEmpty()) return;

        List<JoinRequest> toPromote = waitlisted.stream().limit(available).toList();

        for (JoinRequest jr : toPromote) {
            jr.setStatus(JoinRequestStatus.APPROVED);
            jr.setRespondedAt(Instant.now());
            jr.setWaitlistPosition(null);

            GroupMember member = new GroupMember();
            member.setEvent(event);
            member.setUser(jr.getRequester());
            member.setRole(MemberRole.MEMBER);
            member.setStatus(MemberStatus.ACTIVE);
            groupMemberRepository.save(member);

            event.setCurrentMemberCount(event.getCurrentMemberCount() + 1);

            log.info("Promoted user {} from waitlist for event {}", jr.getRequester().getId(), eventId);
            eventPublisher.publishEvent(new JoinRequestApprovedEvent(
                    eventId, jr.getRequester().getId(), event.getCreator().getId(), true));
        }

        joinRequestRepository.saveAll(toPromote);
        reindexWaitlist(eventId);

        event.setStatus(event.getCurrentMemberCount() >= event.getMaxCompanions()
                ? EventStatus.FULL : EventStatus.OPEN);
        eventRepository.save(event);
    }

    private void reindexWaitlist(UUID eventId) {
        List<JoinRequest> remaining = joinRequestRepository
                .findByEventIdAndStatusOrderByWaitlistPositionAsc(eventId, JoinRequestStatus.WAITLISTED);
        for (int i = 0; i < remaining.size(); i++) {
            remaining.get(i).setWaitlistPosition(i + 1);
        }
        if (!remaining.isEmpty()) {
            joinRequestRepository.saveAll(remaining);
        }
    }
}
