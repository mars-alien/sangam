package com.gatherup.scheduler;

import com.gatherup.domain.Event;
import com.gatherup.domain.enums.EventStatus;
import com.gatherup.repository.EventRepository;
import com.gatherup.repository.RefreshTokenRepository;
import com.gatherup.service.JoinRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventLifecycleScheduler {

    private final EventRepository eventRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JoinRequestService joinRequestService;

    @Scheduled(fixedDelay = 300_000)
    public void promoteWaitlistedMembers() {
        List<Event> eligible = eventRepository.findEventsWithAvailableSlotsAndWaitlist(
                List.of(EventStatus.OPEN, EventStatus.FULL));

        if (eligible.isEmpty()) return;

        log.info("Waitlist promotion: checking {} event(s)", eligible.size());
        eligible.forEach(e -> joinRequestService.promoteFromWaitlist(e.getId()));
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void transitionEventStatuses() {
        Instant now = Instant.now();

        // OPEN/FULL events whose start time has passed → ONGOING
        List<Event> toOngoing = eventRepository.findByStatusInAndEventDateBefore(
                List.of(EventStatus.OPEN, EventStatus.FULL), now);
        toOngoing.forEach(e -> e.setStatus(EventStatus.ONGOING));
        if (!toOngoing.isEmpty()) {
            eventRepository.saveAll(toOngoing);
            log.info("Transitioned {} event(s) to ONGOING", toOngoing.size());
        }

        // ONGOING events whose end time has passed → COMPLETED
        List<Event> toCompleted = eventRepository.findEventsToComplete(now, now.minus(3, ChronoUnit.HOURS));
        toCompleted.forEach(e -> e.setStatus(EventStatus.COMPLETED));
        if (!toCompleted.isEmpty()) {
            eventRepository.saveAll(toCompleted);
            log.info("Transitioned {} event(s) to COMPLETED", toCompleted.size());
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredData() {
        // Revoke expired refresh tokens
        refreshTokenRepository.deleteAllByExpiresAtBefore(Instant.now());

        // Soft-delete CANCELLED/COMPLETED events older than 30 days
        Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);
        List<Event> stale = eventRepository.findStaleEventsForCleanup(
                List.of(EventStatus.CANCELLED, EventStatus.COMPLETED), cutoff);

        Instant now = Instant.now();
        stale.forEach(e -> e.setDeletedAt(now));
        if (!stale.isEmpty()) {
            eventRepository.saveAll(stale);
            log.info("Soft-deleted {} stale event(s)", stale.size());
        }
    }
}
