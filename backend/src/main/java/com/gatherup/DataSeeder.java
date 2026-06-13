package com.gatherup;

import com.gatherup.domain.Event;
import com.gatherup.domain.GroupMember;
import com.gatherup.domain.JoinRequest;
import com.gatherup.domain.User;
import com.gatherup.domain.enums.EventCategory;
import com.gatherup.domain.enums.EventStatus;
import com.gatherup.domain.enums.JoinRequestStatus;
import com.gatherup.domain.enums.MemberRole;
import com.gatherup.domain.enums.MemberStatus;
import com.gatherup.domain.enums.Role;
import com.gatherup.repository.EventRepository;
import com.gatherup.repository.GroupMemberRepository;
import com.gatherup.repository.JoinRequestRepository;
import com.gatherup.repository.UserRepository;
import com.gatherup.util.GeometryUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final JoinRequestRepository joinRequestRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail("alice@demo.com")) {
            return; // already seeded
        }

        String hash = passwordEncoder.encode("password123");

        User alice = createUser("alice",   "alice@demo.com",   hash, "Bengaluru");
        User bob   = createUser("bob",     "bob@demo.com",     hash, "Bengaluru");
        User carol = createUser("carol",   "carol@demo.com",   hash, "Bengaluru");

        // Koramangala — tech meetup
        Event techEvent = createEvent(alice,
                "Spring Boot & Microservices Workshop",
                "Deep-dive into building production-grade Spring Boot apps. Bring your laptop.",
                EventCategory.TECH, 12.9352, 77.6245,
                "91springboard Koramangala", "5th Block, Koramangala", "Bengaluru",
                daysFromNow(5), 2, 15,
                Set.of("java", "spring", "backend"));

        // Indiranagar — music jam
        Event musicEvent = createEvent(bob,
                "Indie Music Jam Session",
                "Open jam for indie musicians and curious listeners. All instruments welcome.",
                EventCategory.MUSIC, 12.9784, 77.6408,
                "Fandom Café, Indiranagar", "12th Main, Indiranagar", "Bengaluru",
                daysFromNow(3), 1, 10,
                Set.of("music", "live", "indie"));

        // Church Street — food walk
        Event foodEvent = createEvent(carol,
                "Church Street Food Walk",
                "Explore the best street food and cafés along Church Street. We pay separately.",
                EventCategory.FOOD, 12.9768, 77.5948,
                "Church Street", "Church Street, MG Road", "Bengaluru",
                daysFromNow(2), 2, 8,
                Set.of("food", "street-food", "social"));

        // Cubbon Park — morning run
        Event sportsEvent = createEvent(alice,
                "Cubbon Park Morning Run",
                "5 km friendly run through Cubbon Park. All paces welcome, no race.",
                EventCategory.SPORTS, 12.9769, 77.5930,
                "Cubbon Park Main Gate", "Cubbon Park", "Bengaluru",
                daysFromNow(1), 1, 20,
                Set.of("running", "fitness", "outdoor"));

        // Lalbagh — botanical walk
        Event outdoorEvent = createEvent(bob,
                "Lalbagh Botanical Photography Walk",
                "Slow-paced walk with photography stops. Any camera is fine — phone cameras too.",
                EventCategory.OUTDOOR, 12.9507, 77.5848,
                "Lalbagh Botanical Garden", "Lalbagh Road", "Bengaluru",
                daysFromNow(4), 1, 12,
                Set.of("photography", "nature", "walking"));

        // Bob requests to join Alice's tech event
        JoinRequest jr1 = new JoinRequest();
        jr1.setEvent(techEvent);
        jr1.setRequester(bob);
        jr1.setMessage("Big Spring Boot fan — would love to attend!");
        jr1.setStatus(JoinRequestStatus.PENDING);
        joinRequestRepository.save(jr1);

        // Carol requests to join Alice's morning run
        JoinRequest jr2 = new JoinRequest();
        jr2.setEvent(sportsEvent);
        jr2.setRequester(carol);
        jr2.setMessage("Looking for a running buddy — I do 6 min/km pace.");
        jr2.setStatus(JoinRequestStatus.PENDING);
        joinRequestRepository.save(jr2);

        // Alice requests to join Bob's music jam
        JoinRequest jr3 = new JoinRequest();
        jr3.setEvent(musicEvent);
        jr3.setRequester(alice);
        jr3.setMessage("I play guitar — hope that's OK!");
        jr3.setStatus(JoinRequestStatus.APPROVED);
        joinRequestRepository.save(jr3);

        log.info("Demo data seeded — alice@demo.com / bob@demo.com / carol@demo.com  (password: password123)");
    }

    private User createUser(String username, String email, String passwordHash, String city) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPasswordHash(passwordHash);
        u.setCity(city);
        u.setRoles(Set.of(Role.ROLE_USER));
        u.setLastActiveAt(Instant.now());
        return userRepository.save(u);
    }

    private Event createEvent(User creator, String title, String description,
                              EventCategory category, double lat, double lng,
                              String venueName, String address, String city,
                              Instant eventDate, int minCompanions, int maxCompanions,
                              Set<String> tags) {
        Event e = new Event();
        e.setTitle(title);
        e.setDescription(description);
        e.setCreator(creator);
        e.setCategory(category);
        e.setLocation(GeometryUtils.createPoint(lat, lng));
        e.setVenueName(venueName);
        e.setAddress(address);
        e.setCity(city);
        e.setEventDate(eventDate);
        e.setMinCompanions(minCompanions);
        e.setMaxCompanions(maxCompanions);
        e.setTags(tags);
        e.setStatus(EventStatus.OPEN);
        e.setCurrentMemberCount(1);
        Event saved = eventRepository.save(e);

        GroupMember creatorMember = new GroupMember();
        creatorMember.setEvent(saved);
        creatorMember.setUser(creator);
        creatorMember.setRole(MemberRole.CREATOR);
        creatorMember.setStatus(MemberStatus.ACTIVE);
        groupMemberRepository.save(creatorMember);

        return saved;
    }

    private static Instant daysFromNow(int days) {
        return Instant.now().plus(days, ChronoUnit.DAYS);
    }
}
