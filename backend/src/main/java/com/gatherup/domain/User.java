package com.gatherup.domain;

import com.gatherup.domain.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_users_email",    columnList = "email"),
        @Index(name = "idx_users_username", columnList = "username")
    }
)
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
public class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(length = 500)
    private String avatarUrl;

    @Column(length = 500)
    private String bio;

    @Column(length = 100)
    private String city;

    @Column
    private Instant deletedAt;

    @Column
    private Instant lastActiveAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "roles")
    private Set<Role> roles = new HashSet<>();

    @OneToMany(mappedBy = "creator")
    @ToString.Exclude
    private List<Event> createdEvents = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    @ToString.Exclude
    private List<GroupMember> groupMemberships = new ArrayList<>();
}
