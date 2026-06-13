package com.gatherup.service;

import com.gatherup.domain.RefreshToken;
import com.gatherup.domain.User;
import com.gatherup.domain.enums.Role;
import com.gatherup.dto.request.LoginRequest;
import com.gatherup.dto.request.RegisterRequest;
import com.gatherup.dto.response.AuthResponse;
import com.gatherup.dto.response.UserProfileResponse;
import com.gatherup.exception.DuplicateResourceException;
import com.gatherup.exception.UnauthorizedException;
import com.gatherup.repository.RefreshTokenRepository;
import com.gatherup.repository.UserRepository;
import com.gatherup.security.JwtProperties;
import com.gatherup.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new DuplicateResourceException("Email already registered: " + req.email());
        }
        if (userRepository.existsByUsername(req.username())) {
            throw new DuplicateResourceException("Username already taken: " + req.username());
        }

        User user = new User();
        user.setUsername(req.username());
        user.setEmail(req.email());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRoles(Set.of(Role.ROLE_USER));
        user.setLastActiveAt(Instant.now());
        user = userRepository.save(user);

        log.info("User registered: {}", user.getEmail());
        return issueTokenPair(user);
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(req.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        user.setLastActiveAt(Instant.now());
        log.info("User logged in: {}", user.getEmail());
        return issueTokenPair(user);
    }

    public AuthResponse refreshToken(String rawToken) {
        String hashed = hash(rawToken);
        RefreshToken stored = refreshTokenRepository.findByTokenAndRevokedFalse(hashed)
                .filter(rt -> rt.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token"));

        stored.setRevoked(true);
        User user = stored.getUser();
        log.info("Refresh token rotated for user: {}", user.getEmail());
        return issueTokenPair(user);
    }

    public void logout(String rawToken) {
        String hashed = hash(rawToken);
        refreshTokenRepository.findByTokenAndRevokedFalse(hashed)
                .ifPresent(rt -> rt.setRevoked(true));
    }

    private AuthResponse issueTokenPair(User user) {
        String accessToken  = jwtService.generateAccessToken(user);
        String rawRefresh   = jwtService.generateRefreshToken();
        Instant accessExp   = Instant.now().plusMillis(jwtProperties.getAccessExpiryMs());
        Instant refreshExp  = Instant.now().plusMillis(jwtProperties.getRefreshExpiryMs());

        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setToken(hash(rawRefresh));
        rt.setExpiresAt(refreshExp);
        refreshTokenRepository.save(rt);

        UserProfileResponse profile = new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getAvatarUrl(),
                user.getBio(),
                user.getCity(),
                0,
                user.getCreatedAt()
        );

        return new AuthResponse(accessToken, rawRefresh, accessExp, profile);
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
