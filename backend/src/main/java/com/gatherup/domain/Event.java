package com.gatherup.domain;

import com.gatherup.domain.enums.EventCategory;
import com.gatherup.domain.enums.EventStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(
    name = "events",
    indexes = {
        @Index(name = "idx_events_creator", columnList = "creator_id"),
        @Index(name = "idx_events_status",  columnList = "status"),
        @Index(name = "idx_events_date",    columnList = "event_date")
    }
)
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
public class Event extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    @ToString.Exclude
    private User creator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status = EventStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventCategory category;

    @Column(columnDefinition = "geometry(Point,4326)")
    private Point location;

    @Column(nullable = false, length = 200)
    private String venueName;

    @Column(length = 500)
    private String address;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false)
    private Instant eventDate;

    @Column
    private Instant eventEndDate;

    @Min(1)
    @Column(nullable = false)
    private int minCompanions = 1;

    @Min(1)
    @Column(nullable = false)
    private int maxCompanions;

    @Column(nullable = false)
    private int currentMemberCount = 0;

    @Column(length = 500)
    private String imageUrl;

    @Column
    private Instant deletedAt;

    @ElementCollection
    @CollectionTable(name = "event_tags", joinColumns = @JoinColumn(name = "event_id"))
    @Column(name = "tags", length = 50)
    private Set<String> tags = new HashSet<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL)
    @ToString.Exclude
    private List<GroupMember> members = new ArrayList<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL)
    @ToString.Exclude
    private List<JoinRequest> joinRequests = new ArrayList<>();
}
