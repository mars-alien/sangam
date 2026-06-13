package com.gatherup.integration;

import com.gatherup.dto.request.LoginRequest;
import com.gatherup.dto.request.RefreshTokenRequest;
import com.gatherup.dto.request.RegisterRequest;
import com.gatherup.dto.response.AuthResponse;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(
                    DockerImageName.parse("postgis/postgis:15-3.3")
                            .asCompatibleSubstituteFor("postgres"))
                    .withDatabaseName("sangam_test")
                    .withUsername("test")
                    .withPassword("test");

    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>("redis:7-alpine")
                    .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "");
    }

    @Autowired
    private TestRestTemplate rest;

    // Shared across ordered test steps
    private static String accessToken;
    private static String refreshToken;

    // ── 1. Register ───────────────────────────────────────────────────────────

    @Test
    @Order(1)
    void register_returnsTokens() {
        var req = new RegisterRequest("testuser", "test@example.com", "password123");
        ResponseEntity<Map<String, Object>> resp = rest.exchange(
                "/api/v1/auth/register",
                HttpMethod.POST,
                json(req),
                new ParameterizedTypeReference<>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo("success");

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        assertThat(data).containsKeys("accessToken", "refreshToken");
        accessToken  = (String) data.get("accessToken");
        refreshToken = (String) data.get("refreshToken");
    }

    // ── 2. Login ──────────────────────────────────────────────────────────────

    @Test
    @Order(2)
    void login_returnsTokens() {
        var req = new LoginRequest("test@example.com", "password123");
        ResponseEntity<Map<String, Object>> resp = rest.exchange(
                "/api/v1/auth/login",
                HttpMethod.POST,
                json(req),
                new ParameterizedTypeReference<>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) resp.getBody().get("data");
        // Overwrite with fresh tokens from login
        accessToken  = (String) data.get("accessToken");
        refreshToken = (String) data.get("refreshToken");
        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();
    }

    // ── 3. Access protected endpoint ─────────────────────────────────────────

    @Test
    @Order(3)
    void myEvents_withValidToken_returns200() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        ResponseEntity<Map<String, Object>> resp = rest.exchange(
                "/api/v1/events/my",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ── 4. Refresh token ──────────────────────────────────────────────────────

    @Test
    @Order(4)
    void refresh_returnsNewTokenPair() {
        var req = new RefreshTokenRequest(refreshToken);
        ResponseEntity<Map<String, Object>> resp = rest.exchange(
                "/api/v1/auth/refresh",
                HttpMethod.POST,
                json(req),
                new ParameterizedTypeReference<>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) resp.getBody().get("data");
        String newAccess  = (String) data.get("accessToken");
        String newRefresh = (String) data.get("refreshToken");
        assertThat(newAccess).isNotBlank();
        // Rotated — new refresh token must differ from the old one
        assertThat(newRefresh).isNotEqualTo(refreshToken);
        refreshToken = newRefresh;
    }

    // ── 5. Logout ─────────────────────────────────────────────────────────────

    @Test
    @Order(5)
    void logout_invalidatesRefreshToken() {
        // Logout
        var req = new RefreshTokenRequest(refreshToken);
        ResponseEntity<Map<String, Object>> logoutResp = rest.exchange(
                "/api/v1/auth/logout",
                HttpMethod.POST,
                json(req),
                new ParameterizedTypeReference<>() {});
        assertThat(logoutResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Attempting to refresh with the now-revoked token must fail
        ResponseEntity<Map<String, Object>> retryResp = rest.exchange(
                "/api/v1/auth/refresh",
                HttpMethod.POST,
                json(req),
                new ParameterizedTypeReference<>() {});
        assertThat(retryResp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private <T> HttpEntity<T> json(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
