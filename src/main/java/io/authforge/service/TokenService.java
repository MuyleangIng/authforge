package io.authforge.service;

import io.authforge.domain.Realm;
import io.authforge.domain.User;
import io.authforge.security.JwtTokenProvider;
import io.authforge.security.RedisTokenCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Generates and validates JWT access tokens and refresh tokens.
 * All validation results are cached in Redis to minimise database round-trips.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {

    private final JwtTokenProvider tokenProvider;
    private final RedisTokenCache tokenCache;

    /**
     * Generate an access token for the given user in the given realm.
     *
     * @return signed JWT string
     */
    public String generateAccessToken(User user, Realm realm) {
        List<String> roles = user.getRoles().stream()
                .map(r -> r.getName())
                .toList();

        String token = tokenProvider.generateAccessToken(
                user.getId().toString(),
                realm.getName(),
                roles,
                realm.getAccessTokenLifespan());

        // Prime the Redis cache so the first request hits cache instead of crypto
        tokenProvider.extractJti(token).ifPresent(jti ->
                tokenCache.cacheValid(jti, realm.getAccessTokenLifespan()));

        return token;
    }

    /**
     * Generate a refresh token for the given user/realm pair.
     */
    public String generateRefreshToken(User user, Realm realm) {
        return tokenProvider.generateRefreshToken(
                user.getId().toString(),
                realm.getName(),
                realm.getRefreshTokenLifespan());
    }

    /**
     * Validate a raw token string.
     * Checks Redis blacklist first, then cryptographic signature.
     *
     * @return true if the token is valid and not revoked
     */
    public boolean validateToken(String token) {
        Optional<String> jtiOpt = tokenProvider.extractJti(token);

        if (jtiOpt.isPresent()) {
            String jti = jtiOpt.get();

            if (tokenCache.isRevoked(jti)) {
                log.debug("Token jti={} is revoked", jti);
                return false;
            }

            if (tokenCache.isValid(jti)) {
                return true;
            }
        }

        return tokenProvider.validateToken(token);
    }

    /**
     * Revoke (blacklist) an access token so it will be rejected by the filter.
     */
    public void revokeToken(String token) {
        tokenProvider.extractJti(token).ifPresent(jti -> {
            try {
                long remaining = (tokenProvider.parseToken(token).getExpiration().getTime()
                        - System.currentTimeMillis()) / 1000;
                tokenCache.revoke(jti, Math.max(remaining, 0));
                log.debug("Token jti={} revoked", jti);
            } catch (Exception e) {
                // Token already expired — nothing to revoke
                log.debug("Could not revoke already-expired token jti={}", jti);
            }
        });
    }

    /**
     * Build the standard token response map for the /token endpoint.
     */
    /**
     * Delegate raw token parsing to the provider (used by AuthService for Claims extraction).
     */
    public io.jsonwebtoken.Claims parseToken(String token) {
        return tokenProvider.parseToken(token);
    }

    public Map<String, Object> buildTokenResponse(String accessToken, String refreshToken, Realm realm) {
        return Map.of(
                "access_token", accessToken,
                "token_type", "Bearer",
                "expires_in", realm.getAccessTokenLifespan(),
                "refresh_token", refreshToken,
                "refresh_expires_in", realm.getRefreshTokenLifespan()
        );
    }
}
