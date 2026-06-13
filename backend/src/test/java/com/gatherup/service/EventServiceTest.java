package com.gatherup.service;

import com.gatherup.domain.Event;
import com.gatherup.domain.User;
import com.gatherup.domain.enums.EventCategory;
import com.gatherup.domain.enums.EventStatus;
import com.gatherup.domain.enums.MemberStatus;
import com.gatherup.dto.request.CreateEventRequest;
import com.gatherup.exception.DuplicateResourceException;
import com.gatherup.exception.InvalidOperationException;
import com.gatherup.mapper.EventMapper;
import com.gatherup.mapper.JoinRequestMapper;
import com.gatherup.mapper.UserMapper;
import com.gatherup.repository.EventRepository;
import com.gatherup.repository.GroupMemberRepository;
import com.gatherup.repository.JoinRequestRepository;
import com.gatherup.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock private EventRepository eventRepository;
    @Mock private UserRepository userRepository;
    @Mock private GroupMemberRepository groupMemberRepository;
    @Mock private JoinRequestRepository joinRequestRepository;
    @Mock private EventMapper eventMapper;
    @Mock private UserMapper userMapper;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private JoinRequestMapper joinRequestMapper;

    private EventService eventService;
    private JoinRequestService joinRequestService;

    private final UUID creatorId   = UUID.randomUUID();
    private final UUID requesterId = UUID.randomUUID();
    private final UUID eventId     = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        eventService = new EventService(
                eventRepository, userRepository, groupMemberRepository,
                joinRequestRepository, eventMapper, eventPublisher, userMapper);

        joinRequestService = new JoinRequestService(
                joinRequestRepository, groupMemberRepository, eventRepository,
                userRepository, joinRequestMapper, eventPublisher);
    }

    // ── EventService tests ────────────────────────────────────────────────────

    @Nested
    @DisplayName("createEvent")
    class CreateEventTests {

        @Test
        @DisplayName("rejects eventDate in the past")
        void pastDate_throwsInvalidOperation() {
            var user = new User();
            when(userRepository.findById(creatorId)).thenReturn(Optional.of(user));

            var req = validRequestBuilder()
                    .eventDate(Instant.now().minusSeconds(3600))
                    .build();

            assertThatThrownBy(() -> eventService.createEvent(req, creatorId))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("future");
        }

        @Test
        @DisplayName("rejects maxCompanions < minCompanions")
        void invalidCompanionRange_throwsInvalidOperation() {
            var user = new User();
            when(userRepository.findById(creatorId)).thenReturn(Optional.of(user));

            var req = validRequestBuilder()
                    .minCompanions(5)
                    .maxCompanions(3)
                    .build();

            assertThatThrownBy(() -> eventService.createEvent(req, creatorId))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("maxCompanions");
        }
    }

    // ── JoinRequestService tests ──────────────────────────────────────────────

    @Nested
    @DisplayName("sendJoinRequest")
    class SendJoinRequestTests {

        @Test
        @DisplayName("rejects request to own event")
        void ownEvent_throwsInvalidOperation() {
            Event event = openEvent(creatorId); // requester == creator
            when(eventRepository.findByIdWithDetails(eventId)).thenReturn(Optional.of(event));

            assertThatThrownBy(() -> joinRequestService.sendJoinRequest(eventId, creatorId, "hi"))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("creator");
        }

        @Test
        @DisplayName("rejects when requester is already an active member")
        void alreadyMember_throwsDuplicateResource() {
            Event event = openEvent(UUID.randomUUID()); // different creator
            when(eventRepository.findByIdWithDetails(eventId)).thenReturn(Optional.of(event));
            when(joinRequestRepository.findByEventIdAndRequesterId(eventId, requesterId))
                    .thenReturn(Optional.empty());
            when(groupMemberRepository.existsByEventIdAndUserIdAndStatus(eventId, requesterId, MemberStatus.ACTIVE))
                    .thenReturn(true);

            assertThatThrownBy(() -> joinRequestService.sendJoinRequest(eventId, requesterId, "hi"))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("already a member");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Event openEvent(UUID creatorId) {
        User creator = new User();
        creator.setId(creatorId);
        Event event = new Event();
        event.setCreator(creator);
        event.setStatus(EventStatus.OPEN);
        event.setMaxCompanions(10);
        event.setCurrentMemberCount(1);
        return event;
    }

    private EventRequestBuilder validRequestBuilder() {
        return new EventRequestBuilder();
    }

    /** Simple builder so individual fields can be overridden per test. */
    static class EventRequestBuilder {
        private Instant eventDate = Instant.now().plus(7, ChronoUnit.DAYS);
        private int minCompanions = 1;
        private int maxCompanions = 5;

        EventRequestBuilder eventDate(Instant d) { this.eventDate = d; return this; }
        EventRequestBuilder minCompanions(int n) { this.minCompanions = n; return this; }
        EventRequestBuilder maxCompanions(int n) { this.maxCompanions = n; return this; }

        CreateEventRequest build() {
            return new CreateEventRequest(
                    "Test Event",
                    "A description for the test event",
                    EventCategory.SOCIAL,
                    12.9716,
                    77.5946,
                    "Test Venue",
                    "123 Test Street",
                    "Bengaluru",
                    eventDate,
                    null,
                    minCompanions,
                    maxCompanions,
                    null
            );
        }
    }
}
