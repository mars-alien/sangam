package com.gatherup.util;

import com.gatherup.domain.User;
import com.gatherup.exception.UnauthorizedException;
import com.gatherup.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    public UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new UnauthorizedException("No authenticated user");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails ud) {
            return UUID.fromString(ud.getUsername());
        }
        throw new UnauthorizedException("Cannot resolve user from security context");
    }

    public User getCurrentUser() {
        UUID userId = getCurrentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Authenticated user not found"));
    }
}
