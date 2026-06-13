package com.gatherup.repository;

import com.gatherup.domain.Event;
import com.gatherup.domain.GroupMember;
import com.gatherup.domain.enums.MemberRole;
import com.gatherup.domain.enums.MemberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {

    Optional<GroupMember> findByEventIdAndUserId(UUID eventId, UUID userId);

    boolean existsByEventIdAndUserId(UUID eventId, UUID userId);

    boolean existsByEventIdAndUserIdAndStatus(UUID eventId, UUID userId, MemberStatus status);

    List<GroupMember> findByEventIdAndStatus(UUID eventId, MemberStatus status);

    Page<GroupMember> findByEventIdAndStatus(UUID eventId, MemberStatus status, Pageable pageable);

    @Query("""
            SELECT gm.event FROM GroupMember gm
            WHERE gm.user.id = :userId
              AND gm.status = :status
              AND gm.role != :excludeRole
              AND gm.event.deletedAt IS NULL
            """)
    List<Event> findJoinedEventsByUserIdAndStatus(UUID userId, MemberStatus status, MemberRole excludeRole);
}
