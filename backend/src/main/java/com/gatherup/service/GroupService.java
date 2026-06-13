package com.gatherup.service;

import com.gatherup.domain.Event;
import com.gatherup.domain.GroupMember;
import com.gatherup.domain.JoinRequest;
import com.gatherup.domain.enums.EventStatus;
import com.gatherup.domain.enums.JoinRequestStatus;
import com.gatherup.domain.enums.MemberRole;
import com.gatherup.domain.enums.MemberStatus;
import com.gatherup.dto.response.GroupMemberResponse;
import com.gatherup.dto.response.PageResponse;
import com.gatherup.event.MemberLeftEvent;
import com.gatherup.exception.InvalidOperationException;
import com.gatherup.exception.ResourceNotFoundException;
import com.gatherup.exception.UnauthorizedException;
import com.gatherup.mapper.GroupMemberMapper;
import com.gatherup.repository.EventRepository;
import com.gatherup.repository.GroupMemberRepository;
import com.gatherup.repository.JoinRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupMemberRepository groupMemberRepository;
    private final JoinRequestRepository joinRequestRepository;
    private final EventRepository eventRepository;
    private final JoinRequestService joinRequestService;
    private final GroupMemberMapper groupMemberMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public PageResponse<GroupMemberResponse> getEventMembers(UUID eventId, Pageable pageable) {
        Page<GroupMember> page = groupMemberRepository.findByEventIdAndStatus(
                eventId, MemberStatus.ACTIVE, pageable);
        return PageResponse.from(page.map(groupMemberMapper::toResponse));
    }

    @Transactional
    public void leaveEvent(UUID eventId, UUID userId) {
        GroupMember member = groupMemberRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("You are not a member of this event"));

        if (member.getRole() == MemberRole.CREATOR) {
            throw new InvalidOperationException("The creator cannot leave — cancel the event instead");
        }
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new InvalidOperationException("You are no longer an active member of this event");
        }

        member.setStatus(MemberStatus.LEFT);
        groupMemberRepository.save(member);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));
        event.setCurrentMemberCount(Math.max(0, event.getCurrentMemberCount() - 1));
        if (event.getStatus() == EventStatus.FULL) {
            event.setStatus(EventStatus.OPEN);
        }
        eventRepository.save(event);

        // Mark the user's APPROVED join request as CANCELLED (keeps history clean)
        joinRequestRepository.findByEventIdAndRequesterId(eventId, userId)
                .filter(jr -> jr.getStatus() == JoinRequestStatus.APPROVED)
                .ifPresent(jr -> {
                    jr.setStatus(JoinRequestStatus.CANCELLED);
                    joinRequestRepository.save(jr);
                });

        log.info("User {} left event {}", userId, eventId);
        eventPublisher.publishEvent(new MemberLeftEvent(eventId, userId));

        // Immediately promote from waitlist if there's a slot available
        joinRequestService.promoteFromWaitlist(eventId);
    }

    @Transactional
    public void removeMember(UUID eventId, UUID targetUserId, UUID requestingUserId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));

        if (!event.getCreator().getId().equals(requestingUserId)) {
            throw new UnauthorizedException("Only the event creator can remove members");
        }

        GroupMember member = groupMemberRepository.findByEventIdAndUserId(eventId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User is not a member of this event"));

        if (member.getRole() == MemberRole.CREATOR) {
            throw new InvalidOperationException("Cannot remove the event creator");
        }
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new InvalidOperationException("Member is not currently active");
        }

        member.setStatus(MemberStatus.REMOVED);
        groupMemberRepository.save(member);

        event.setCurrentMemberCount(Math.max(0, event.getCurrentMemberCount() - 1));
        if (event.getStatus() == EventStatus.FULL) {
            event.setStatus(EventStatus.OPEN);
        }
        eventRepository.save(event);

        log.info("User {} removed from event {} by {}", targetUserId, eventId, requestingUserId);
        eventPublisher.publishEvent(new MemberLeftEvent(eventId, targetUserId));

        joinRequestService.promoteFromWaitlist(eventId);
    }
}
