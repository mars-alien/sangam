package com.gatherup.repository;

import com.gatherup.domain.Event;
import com.gatherup.domain.enums.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    // Returns [id(text), distance_km] pairs ordered by distance — service loads actual events.
    // CAST used instead of :: because Hibernate interprets :: as a named-parameter prefix.
    @Query(value = """
            SELECT CAST(e.id AS text),
                   ST_Distance(
                       CAST(e.location AS geography),
                       CAST(ST_MakePoint(:lng, :lat) AS geography)
                   ) / 1000
            FROM events e
            WHERE e.deleted_at IS NULL
              AND e.status IN ('OPEN', 'FULL')
              AND e.event_date > NOW()
              AND ST_DWithin(
                    CAST(e.location AS geography),
                    CAST(ST_MakePoint(:lng, :lat) AS geography),
                    :radiusMeters
                  )
              AND (:category IS NULL OR e.category = :category)
            ORDER BY 2 ASC
            """,
            countQuery = """
            SELECT COUNT(*) FROM events e
            WHERE e.deleted_at IS NULL AND e.status IN ('OPEN','FULL')
              AND e.event_date > NOW()
              AND ST_DWithin(
                    CAST(e.location AS geography),
                    CAST(ST_MakePoint(:lng, :lat) AS geography),
                    :radiusMeters
                  )
              AND (:category IS NULL OR e.category = :category)
            """,
            nativeQuery = true)
    Page<Object[]> findNearbyEventIds(double lat, double lng, double radiusMeters,
                                       String category, Pageable pageable);

    // Returns [id(text), rank(float)] pairs ordered by FTS rank.
    // Two columns keep the result as Object[] — single-column native queries return scalars, not arrays.
    // Uses ?1 positional param — named :query confuses Hibernate when adjacent to the @@ operator.
    @Query(value = """
            SELECT CAST(id AS text),
                   ts_rank(
                       to_tsvector('english', title || ' ' || COALESCE(description, '')),
                       plainto_tsquery('english', ?1)
                   ) AS rank
            FROM events
            WHERE deleted_at IS NULL
              AND status IN ('OPEN', 'FULL')
              AND event_date > NOW()
              AND to_tsvector('english', title || ' ' || COALESCE(description, ''))
                  @@ plainto_tsquery('english', ?1)
            ORDER BY rank DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM events
            WHERE deleted_at IS NULL AND status IN ('OPEN','FULL') AND event_date > NOW()
              AND to_tsvector('english', title || ' ' || COALESCE(description, ''))
                  @@ plainto_tsquery('english', ?1)
            """,
            nativeQuery = true)
    Page<Object[]> searchEventIds(String query, Pageable pageable);

    @Query("SELECT DISTINCT e FROM Event e JOIN FETCH e.creator LEFT JOIN FETCH e.tags WHERE e.id IN :ids AND e.deletedAt IS NULL")
    List<Event> findWithCreatorByIdIn(List<UUID> ids);

    @Query("SELECT e FROM Event e JOIN FETCH e.creator LEFT JOIN FETCH e.tags WHERE e.id = :id AND e.deletedAt IS NULL")
    Optional<Event> findByIdWithDetails(UUID id);

    @EntityGraph(attributePaths = {"creator", "tags"})
    Page<Event> findByCreatorIdAndDeletedAtIsNull(UUID creatorId, Pageable pageable);

    List<Event> findByStatusAndEventDateBefore(EventStatus status, Instant cutoff);

    List<Event> findByStatusAndEventDateBetween(EventStatus status, Instant start, Instant end);

    List<Event> findByStatusInAndEventDateBefore(List<EventStatus> statuses, Instant cutoff);

    @Query("""
            SELECT e FROM Event e WHERE e.status IN :statuses AND e.updatedAt < :cutoff AND e.deletedAt IS NULL
            """)
    List<Event> findStaleEventsForCleanup(List<EventStatus> statuses, Instant cutoff);

    @Query("""
            SELECT DISTINCT e FROM Event e
            WHERE e.status IN :statuses
              AND e.currentMemberCount < e.maxCompanions
              AND e.deletedAt IS NULL
              AND EXISTS (
                  SELECT jr FROM JoinRequest jr
                  WHERE jr.event = e
                    AND jr.status = com.gatherup.domain.enums.JoinRequestStatus.WAITLISTED
              )
            """)
    List<Event> findEventsWithAvailableSlotsAndWaitlist(List<EventStatus> statuses);

    @Query("""
            SELECT e FROM Event e
            WHERE e.status = com.gatherup.domain.enums.EventStatus.ONGOING
              AND (
                (e.eventEndDate IS NOT NULL AND e.eventEndDate < :now)
                OR (e.eventEndDate IS NULL AND e.eventDate < :threeHoursAgo)
              )
              AND e.deletedAt IS NULL
            """)
    List<Event> findEventsToComplete(Instant now, Instant threeHoursAgo);
}
