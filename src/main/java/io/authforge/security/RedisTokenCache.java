package io.authforge.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis-backed cache for JWT validation results.
 *
 * <p>Keys are stored under the prefix {@code authforge:token:<jti>}.
 * A value of {@code "valid"} means the token is active;
 * absence of the key means the token is either expired or revoked.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisTokenCache {

    private static final String KEY_PREFIX = "authforge:token:";
    private static final String BLACKLIST_PREFIX = "authforge:blacklist:";
    private static final String VALUE_VALID = "valid";

    private final StringRedisTemplate redisTemplate;

    @Value("${authforge.token.cache-ttl:300}")
    private long cacheTtlSeconds;

    /**
     * Cache a token jti as valid for the given TTL (seconds).
     */
    public void cacheValid(String jti, long ttlSeconds) {
        String key = KEY_PREFIX + jti;
        redisTemplate.opsForValue().set(key, VALUE_VALID, Duration.ofSeconds(ttlSeconds));
        log.debug("Cached token jti={} for {}s", jti, ttlSeconds);
    }

    /**
     * Check whether a jti is still in the valid cache.
     */
    public boolean isValid(String jti) {
        return VALUE_VALID.equals(redisTemplate.opsForValue().get(KEY_PREFIX + jti));
    }

    /**
     * Revoke a token by removing it from the valid cache and adding it to a
     * short-lived blacklist so that subsequent checks fail fast.
     */
    public void revoke(String jti, long remainingTtlSeconds) {
        redisTemplate.delete(KEY_PREFIX + jti);
        if (remainingTtlSeconds > 0) {
            redisTemplate.opsForValue().set(
                    BLACKLIST_PREFIX + jti,
                    "revoked",
                    Duration.ofSeconds(remainingTtlSeconds));
        }
        log.debug("Revoked token jti={}", jti);
    }

    /**
     * Return true if the jti is explicitly blacklisted (revoked before expiry).
     */
    public boolean isRevoked(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + jti));
    }

    /**
     * Convenience: look up cached validation status.
     *
     * @return Optional containing "valid" or "revoked"; empty if not cached
     */
    public Optional<String> getCachedStatus(String jti) {
        String valid = redisTemplate.opsForValue().get(KEY_PREFIX + jti);
        if (valid != null) return Optional.of(valid);
        String revoked = redisTemplate.opsForValue().get(BLACKLIST_PREFIX + jti);
        if (revoked != null) return Optional.of("revoked");
        return Optional.empty();
    }
}
