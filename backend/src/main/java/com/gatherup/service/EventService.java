package com.gatherup.service;

import com.gatherup.domain.Event;
import com.gatherup.domain.GroupMember;
import com.gatherup.domain.JoinRequest;
import com.gatherup.domain.User;
import com.gatherup.domain.enums.EventStatus;
import com.gatherup.domain.enums.JoinRequestStatus;
import com.gatherup.domain.enums.MemberRole;
import com.gatherup.domain.enums.MemberStatus;
import com.gatherup.dto.request.CreateEventRequest;
import com.gatherup.dto.request.UpdateEventRequest;
import com.gatherup.dto.response.EventDetailResponse;
import com.gatherup.dto.response.EventSummaryResponse;
import com.gatherup.dto.response.PageResponse;
import com.gatherup.event.EventCancelledEvent;
import com.gatherup.exception.InvalidOperationException;
import com.gatherup.exception.ResourceNotFoundException;
import com.gatherup.exception.UnauthorizedException;
import com.gatherup.mapper.EventMapper;
import com.gatherup.repository.EventRepository;
import com.gatherup.repository.GroupMemberRepository;
import com.gatherup.repository.JoinRequestRepository;
import com.gatherup.repository.UserRepository;
import com.gatherup.util.GeometryUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final JoinRequestRepository joinRequestRepository;
    private final EventMapper eventMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final com.gatherup.mapper.UserMapper userMapper;

    @Transactional
    public EventDetailResponse createEvent(CreateEventRequest req, UUID creatorId) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", creatorId));

        if (!req.eventDate().isAfter(Instant.now())) {
            throw new InvalidOperationException("Event date must be in the future");
        }
        if (req.maxCompanions() < req.minCompanions()) {
            throw new InvalidOperationException("maxCompanions must be >= minCompanions");
        }

        Event event = new Event();
        event.setTitle(req.title());
        event.setDescription(req.description());
        event.setCreator(creator);
        event.setCategory(req.category());
        event.setLocation(GeometryUtils.createPoint(req.latitude(), req.longitude()));
        event.setVenueName(req.venueName());
        event.setAddress(req.address());
        event.setCity(req.city());
        event.setEventDate(req.eventDate());
        event.setEventEndDate(req.eventEndDate());
        event.setMinCompanions(req.minCompanions());
        event.setMaxCompanions(req.maxCompanions());
        event.setStatus(EventStatus.OPEN);
        if (req.tags() != null) {
            event.setTags(req.tags());
        }
        event = eventRepository.save(event);

        GroupMember creatorMember = new GroupMember();
        creatorMember.setEvent(event);
        creatorMember.setUser(creator);
        creatorMember.setRole(MemberRole.CREATOR);
        creatorMember.setStatus(MemberStatus.ACTIVE);
        groupMemberRepository.save(creatorMember);

        event.setCurrentMemberCount(1);
        event = eventRepository.save(event);

        log.info("Event created: {} by user: {}", event.getId(), creatorId);

        EventDetailResponse response = eventMapper.toDetailResponse(event);
        response.setIsCreator(true);
        response.setCurrentUserStatus("APPROVED");
        return response;
    }

    @Transactional(readOnly = true)
    public EventDetailResponse getEvent(UUID eventId, UUID currentUserId) {
        Event event = eventRepository.findByIdWithDetails(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));

        EventDetailResponse response = eventMapper.toDetailResponse(event);

        if (currentUserId != null) {
            boolean isCreator = event.getCreator().getId().equals(currentUserId);
            response.setIsCreator(isCreator);

            String status = "NONE";
            Integer waitlistPos = null;

            if (isCreator) {
                status = "APPROVED";
            } else {
                Optional<JoinRequest> jr =
                        joinRequestRepository.findByEventIdAndRequesterId(eventId, currentUserId);
                if (jr.isPresent()) {
                    status = jr.get().getStatus().name();
                    waitlistPos = jr.get().getWaitlistPosition();
                }
            }

            response.setCurrentUserStatus(status);
            response.setWaitlistPosition(waitlistPos);
        }

        return response;
    }

    @Cacheable(value = "event-listings",
               key = "#lat + ',' + #lng + ',' + #radiusKm + ',' + #category + ',' + #pageable.pageNumber + ',' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public PageResponse<EventSummaryResponse> getNearbyEvents(double lat, double lng,
                                                               double radiusKm, String category,
                                                               Pageable pageable) {
        // Strip sort — native query handles ordering by distance
        Pageable pageOnly = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Page<Object[]> rawPage = eventRepository.findNearbyEventIds(
                lat, lng, radiusKm * 1000, category, pageOnly);

        if (rawPage.isEmpty()) {
            return PageResponse.<EventSummaryResponse>builder()
                    .content(List.of()).page(rawPage.getNumber()).size(rawPage.getSize())
                    .totalElements(0).totalPages(0).last(true).build();
        }

        List<UUID> orderedIds = new java.util.ArrayList<>();
        Map<UUID, Double> distanceMap = new LinkedHashMap<>();
        for (Object[] row : rawPage.getContent()) {
            UUID id = UUID.fromString(row[0].toString());
            orderedIds.add(id);
            distanceMap.put(id, ((Number) row[1]).doubleValue());
        }

        Map<UUID, Event> eventMap = eventRepository.findWithCreatorByIdIn(orderedIds)
                .stream().collect(Collectors.toMap(Event::getId, Function.identity()));

        List<EventSummaryResponse> items = orderedIds.stream()
                .filter(eventMap::containsKey)
                .map(id -> {
                    Event ev = eventMap.get(id);
                    EventSummaryResponse summary = eventMapper.toSummaryResponse(ev);
                    summary.setDistanceKm(distanceMap.get(id));
                    if (ev.getLocation() != null) {
                        summary.setLatitude(ev.getLocation().getY());
                        summary.setLongitude(ev.getLocation().getX());
                    }
                    return summary;
                })
                .toList();

        return PageResponse.<EventSummaryResponse>builder()
                .content(items)
                .page(rawPage.getNumber())
                .size(rawPage.getSize())
                .totalElements(rawPage.getTotalElements())
                .totalPages(rawPage.getTotalPages())
                .last(rawPage.isLast())
                .build();
    }

    @Cacheable(value = "event-listings", key = "'search:' + #query + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public PageResponse<EventSummaryResponse> searchEvents(String query, Pageable pageable) {
        Pageable pageOnly = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Page<Object[]> rawPage = eventRepository.searchEventIds(query, pageOnly);

        if (rawPage.isEmpty()) {
            return PageResponse.<EventSummaryResponse>builder()
                    .content(List.of()).page(rawPage.getNumber()).size(rawPage.getSize())
                    .totalElements(0).totalPages(0).last(true).build();
        }

        List<UUID> orderedIds = rawPage.getContent().stream()
                .map(row -> UUID.fromString(row[0].toString()))
                .toList();

        Map<UUID, Event> eventMap = eventRepository.findWithCreatorByIdIn(orderedIds)
                .stream().collect(Collectors.toMap(Event::getId, Function.identity()));

        List<EventSummaryResponse> items = orderedIds.stream()
                .filter(eventMap::containsKey)
                .map(id -> {
                    Event ev = eventMap.get(id);
                    EventSummaryResponse summary = eventMapper.toSummaryResponse(ev);
                    if (ev.getLocation() != null) {
                        summary.setLatitude(ev.getLocation().getY());
                        summary.setLongitude(ev.getLocation().getX());
                    }
                    return summary;
                })
                .toList();

        return PageResponse.<EventSummaryResponse>builder()
                .content(items)
                .page(rawPage.getNumber())
                .size(rawPage.getSize())
                .totalElements(rawPage.getTotalElements())
                .totalPages(rawPage.getTotalPages())
                .last(rawPage.isLast())
                .build();
    }

    @Caching(evict = {
            @CacheEvict(value = "event-listings", allEntries = true)
    })
    @Transactional
    public EventDetailResponse updateEvent(UUID eventId, UpdateEventRequest req, UUID currentUserId) {
        Event event = eventRepository.findByIdWithDetails(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));

        if (!event.getCreator().getId().equals(currentUserId)) {
            throw new UnauthorizedException("Only the creator can update this event");
        }
        if (event.getStatus() == EventStatus.COMPLETED || event.getStatus() == EventStatus.CANCELLED) {
            throw new InvalidOperationException("Cannot update a " + event.getStatus() + " event");
        }

        if (req.title() != null) event.setTitle(req.title());
        if (req.description() != null) event.setDescription(req.description());
        if (req.category() != null) event.setCategory(req.category());
        if (req.venueName() != null) event.setVenueName(req.venueName());
        if (req.address() != null) event.setAddress(req.address());
        if (req.city() != null) event.setCity(req.city());
        if (req.eventDate() != null) event.setEventDate(req.eventDate());
        if (req.eventEndDate() != null) event.setEventEndDate(req.eventEndDate());
        if (req.minCompanions() != null) event.setMinCompanions(req.minCompanions());
        if (req.maxCompanions() != null) event.setMaxCompanions(req.maxCompanions());
        if (req.tags() != null) event.setTags(req.tags());
        if (req.latitude() != null && req.longitude() != null) {
            event.setLocation(GeometryUtils.createPoint(req.latitude(), req.longitude()));
        }

        event = eventRepository.save(event);
        log.info("Event updated: {} by user: {}", eventId, currentUserId);

        EventDetailResponse response = eventMapper.toDetailResponse(event);
        response.setIsCreator(true);
        response.setCurrentUserStatus("APPROVED");
        return response;
    }

    @Caching(evict = {
            @CacheEvict(value = "event-listings", allEntries = true)
    })
    @Transactional
    public void deleteEvent(UUID eventId, UUID currentUserId) {
        Event event = eventRepository.findByIdWithDetails(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));

        if (!event.getCreator().getId().equals(currentUserId)) {
            throw new UnauthorizedException("Only the creator can delete this event");
        }

        List<UUID> memberIds = groupMemberRepository.findByEventIdAndStatus(eventId, com.gatherup.domain.enums.MemberStatus.ACTIVE)
                .stream().map(gm -> gm.getUser().getId()).toList();

        event.setDeletedAt(Instant.now());
        event.setStatus(EventStatus.CANCELLED);
        eventRepository.save(event);

        joinRequestRepository.cancelAllActiveForEvent(eventId);

        log.info("Event soft-deleted: {} by user: {}", eventId, currentUserId);
        eventPublisher.publishEvent(new EventCancelledEvent(eventId, memberIds));
    }

    @Transactional(readOnly = true)
    public PageResponse<EventSummaryResponse> getMyEvents(UUID userId, Pageable pageable) {
        // Events created by the user
        List<Event> created = eventRepository
                .findByCreatorIdAndDeletedAtIsNull(userId,
                        PageRequest.of(0, 1000, Sort.by("eventDate").descending()))
                .getContent();

        // Events the user joined (but didn't create)
        List<Event> joined = groupMemberRepository
                .findJoinedEventsByUserIdAndStatus(userId, MemberStatus.ACTIVE, MemberRole.CREATOR);

        // Merge — created first so creator-joined events prefer the created entry
        Map<UUID, Event> merged = new LinkedHashMap<>();
        created.forEach(e -> merged.put(e.getId(), e));
        joined.forEach(e -> merged.putIfAbsent(e.getId(), e));

        List<Event> sorted = merged.values().stream()
                .sorted(Comparator.comparing(Event::getEventDate).reversed())
                .toList();

        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        int start = page * size;
        int end = Math.min(start + size, sorted.size());

        List<EventSummaryResponse> content = (start >= sorted.size())
                ? List.of()
                : sorted.subList(start, end).stream()
                        .map(ev -> {
                            EventSummaryResponse s = eventMapper.toSummaryResponse(ev);
                            if (ev.getLocation() != null) {
                                s.setLatitude(ev.getLocation().getY());
                                s.setLongitude(ev.getLocation().getX());
                            }
                            return s;
                        })
                        .toList();

        return PageResponse.<EventSummaryResponse>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(sorted.size())
                .totalPages((int) Math.ceil((double) sorted.size() / size))
                .last(end >= sorted.size())
                .build();
    }
}
