package com.gatherup.domain;

import com.gatherup.domain.enums.JoinRequestStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

@Entity
@Table(
    name = "join_requests",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_join_requests_event_requester",
        columnNames = {"event_id", "requester_id"}
    ),
    indexes = {
        @Index(name = "idx_join_requests_event",     columnList = "event_id"),
        @Index(name = "idx_join_requests_requester", columnList = "requester_id"),
        @Index(name = "idx_join_requests_status",    columnList = "event_id, status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
public class JoinRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    @ToString.Exclude
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    @ToString.Exclude
    private User requester;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JoinRequestStatus status = JoinRequestStatus.PENDING;

    @Column(length = 300)
    private String message;

    @Column
    private Integer waitlistPosition;

    @Column
    private Instant respondedAt;
}
