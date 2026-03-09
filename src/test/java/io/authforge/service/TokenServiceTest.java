package io.authforge.service;

import io.authforge.domain.Realm;
import io.authforge.domain.Role;
import io.authforge.domain.User;
import io.authforge.security.JwtTokenProvider;
import io.authforge.security.RedisTokenCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenService unit tests")
class TokenServiceTest {

    private static final String SECRET = "test-secret-key-must-be-at-least-32-chars-long!!";
    private static final String ISSUER = "http://localhost:8080";

    private JwtTokenProvider tokenProvider;

    @Mock
    private RedisTokenCache tokenCache;

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(SECRET, ISSUER);
        tokenService  = new TokenService(tokenProvider, tokenCache);
    }

    private User testUser() {
        Role adminRole = Role.builder().id(UUID.randomUUID()).name("admin").realmId(UUID.randomUUID()).build();
        return User.builder()
                .id(UUID.randomUUID())
                .realmId(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .passwordHash("$2a$12$xxx")
                .roles(Set.of(adminRole))
                .build();
    }

    private Realm testRealm() {
        return Realm.builder()
                .id(UUID.randomUUID())
                .name("master")
                .accessTokenLifespan(300)
                .refreshTokenLifespan(1800)
                .build();
    }

    @Test
    @DisplayName("generateAccessToken returns a non-blank JWT")
    void generateAccessToken_returnsToken() {
        User user   = testUser();
        Realm realm = testRealm();

        String token = tokenService.generateAccessToken(user, realm);

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3); // header.payload.signature
    }

    @Test
    @DisplayName("validateToken returns true for a freshly generated token")
    void validateToken_valid() {
        User user   = testUser();
        Realm realm = testRealm();
        when(tokenCache.isValid(any())).thenReturn(false);
        when(tokenCache.isRevoked(any())).thenReturn(false);

        String token = tokenService.generateAccessToken(user, realm);
        boolean valid = tokenService.validateToken(token);

        assertThat(valid).isTrue();
    }

    @Test
    @DisplayName("validateToken returns false for a tampered token")
    void validateToken_tampered() {
        // For a malformed token, extractJti returns empty so neither isRevoked nor isValid is called
        boolean valid = tokenService.validateToken("tampered.token.value");

        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("validateToken returns false for a revoked token (Redis blacklist)")
    void validateToken_revoked() {
        User user   = testUser();
        Realm realm = testRealm();

        String token = tokenService.generateAccessToken(user, realm);

        // Simulate token being in blacklist
        when(tokenCache.isRevoked(any())).thenReturn(true);

        boolean valid = tokenService.validateToken(token);
        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("validateToken uses Redis cache when available (no crypto)")
    void validateToken_usesRedisCache() {
        User user   = testUser();
        Realm realm = testRealm();
        when(tokenCache.isValid(any())).thenReturn(false);

        String token = tokenService.generateAccessToken(user, realm);

        // Now simulate the cache having a valid entry
        when(tokenCache.isRevoked(any())).thenReturn(false);
        when(tokenCache.isValid(any())).thenReturn(true);

        boolean valid = tokenService.validateToken(token);
        assertThat(valid).isTrue();
    }

    @Test
    @DisplayName("buildTokenResponse returns all required OAuth2 fields")
    void buildTokenResponse_hasRequiredFields() {
        Realm realm = testRealm();
        Map<String, Object> resp = tokenService.buildTokenResponse("access", "refresh", realm);

        assertThat(resp).containsKeys("access_token", "token_type", "expires_in", "refresh_token");
        assertThat(resp.get("token_type")).isEqualTo("Bearer");
    }
}
