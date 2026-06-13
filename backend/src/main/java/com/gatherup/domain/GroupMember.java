package com.gatherup.domain;

import com.gatherup.domain.enums.MemberRole;
import com.gatherup.domain.enums.MemberStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

@Entity
@Table(
    name = "group_members",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_group_members_event_user",
        columnNames = {"event_id", "user_id"}
    ),
    indexes = {
        @Index(name = "idx_group_members_event", columnList = "event_id"),
        @Index(name = "idx_group_members_user",  columnList = "user_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
public class GroupMember extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    @ToString.Exclude
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status = MemberStatus.ACTIVE;

    @Column(nullable = false)
    private Instant joinedAt = Instant.now();
}
