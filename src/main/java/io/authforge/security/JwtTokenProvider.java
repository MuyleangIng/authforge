package io.authforge.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Component
@Slf4j
public class JwtTokenProvider {

    private final SecretKey signingKey;
    private final String issuer;

    public JwtTokenProvider(
            @Value("${authforge.jwt.secret}") String secret,
            @Value("${authforge.jwt.issuer}") String issuer) {
        // Ensure the key is at least 256 bits for HS256
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 characters (256 bits)");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.issuer = issuer;
    }

    /**
     * Build an access token.
     *
     * @param subject    user UUID as string
     * @param realmName  realm name used as audience
     * @param roles      list of role names
     * @param ttlSeconds token lifetime in seconds
     * @return signed JWT string
     */
    public String generateAccessToken(String subject, String realmName, List<String> roles, long ttlSeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(issuer + "/realms/" + realmName)
                .subject(subject)
                .audience().add(realmName).and()
                .claim("roles", roles)
                .claim("realm", realmName)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Build a refresh token (opaque — no role claims).
     */
    public String generateRefreshToken(String subject, String realmName, long ttlSeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(issuer + "/realms/" + realmName)
                .subject(subject)
                .claim("type", "refresh")
                .claim("realm", realmName)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Parse and validate a JWT. Returns the Claims on success.
     *
     * @throws JwtException if the token is invalid or expired
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extract subject (user ID) without throwing — returns empty Optional on failure.
     */
    public Optional<String> extractSubject(String token) {
        try {
            return Optional.of(parseToken(token).getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Could not extract subject from token: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Validate token signature and expiry — returns true if valid.
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("Token expired: {}", e.getMessage());
        } catch (JwtException e) {
            log.debug("Invalid token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.debug("Empty or null token");
        }
        return false;
    }

    /**
     * Extract the JWT ID (jti) claim — used as the Redis cache key.
     */
    public Optional<String> extractJti(String token) {
        try {
            return Optional.ofNullable(parseToken(token).getId());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Extract roles claim as a List<String>.
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        try {
            Object roles = parseToken(token).get("roles");
            if (roles instanceof List<?> list) {
                return (List<String>) list;
            }
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Could not extract roles from token");
        }
        return Collections.emptyList();
    }
}
