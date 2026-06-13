package com.gatherup.repository;

import com.gatherup.domain.JoinRequest;
import com.gatherup.domain.enums.JoinRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JoinRequestRepository extends JpaRepository<JoinRequest, UUID> {

    Optional<JoinRequest> findByEventIdAndRequesterId(UUID eventId, UUID requesterId);

    Page<JoinRequest> findByEventIdAndStatus(UUID eventId, JoinRequestStatus status, Pageable pageable);

    List<JoinRequest> findByEventIdAndStatus(UUID eventId, JoinRequestStatus status);

    List<JoinRequest> findByEventIdAndStatusOrderByCreatedAtAsc(UUID eventId, JoinRequestStatus status);

    List<JoinRequest> findByEventIdAndStatusOrderByWaitlistPositionAsc(UUID eventId, JoinRequestStatus status);

    long countByEventIdAndStatus(UUID eventId, JoinRequestStatus status);

    Page<JoinRequest> findByEventId(UUID eventId, Pageable pageable);

    @Query("SELECT MAX(jr.waitlistPosition) FROM JoinRequest jr WHERE jr.event.id = :eventId AND jr.status = com.gatherup.domain.enums.JoinRequestStatus.WAITLISTED")
    Optional<Integer> findMaxWaitlistPositionByEventId(UUID eventId);

    @Modifying
    @Query("""
            UPDATE JoinRequest jr
            SET jr.status = com.gatherup.domain.enums.JoinRequestStatus.CANCELLED
            WHERE jr.event.id = :eventId
              AND jr.status IN (
                  com.gatherup.domain.enums.JoinRequestStatus.PENDING,
                  com.gatherup.domain.enums.JoinRequestStatus.WAITLISTED
              )
            """)
    void cancelAllActiveForEvent(UUID eventId);

    @Modifying
    @Query("""
            UPDATE JoinRequest jr
            SET jr.status = com.gatherup.domain.enums.JoinRequestStatus.CANCELLED
            WHERE jr.event.id = :eventId
              AND jr.status = com.gatherup.domain.enums.JoinRequestStatus.PENDING
            """)
    void cancelAllPendingForEvent(UUID eventId);
}
