package io.authforge.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Extracts a Bearer JWT from the Authorization header, validates it via Redis
 * cache first (fast path), falls back to cryptographic validation, then sets
 * the Spring Security authentication context.
 */
@RequiredArgsConstructor
@Slf4j
public class JwtTokenFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final RedisTokenCache tokenCache;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = extractBearerToken(request);

        if (StringUtils.hasText(token)) {
            try {
                // Fast path: check Redis before doing crypto
                String jti = tokenProvider.extractJti(token).orElse(null);

                if (jti != null && tokenCache.isRevoked(jti)) {
                    log.debug("Token jti={} is revoked — rejecting", jti);
                } else {
                    boolean valid;
                    if (jti != null && tokenCache.isValid(jti)) {
                        // Cache hit — skip signature verification
                        valid = true;
                        log.debug("Token jti={} validated via Redis cache", jti);
                    } else {
                        // Cache miss — full cryptographic validation
                        valid = tokenProvider.validateToken(token);
                        if (valid && jti != null) {
                            Claims claims = tokenProvider.parseToken(token);
                            long remaining = (claims.getExpiration().getTime() - System.currentTimeMillis()) / 1000;
                            tokenCache.cacheValid(jti, Math.max(remaining, 1));
                        }
                    }

                    if (valid) {
                        Claims claims = tokenProvider.parseToken(token);
                        String subject = claims.getSubject();

                        @SuppressWarnings("unchecked")
                        List<String> roles = (List<String>) claims.getOrDefault("roles", List.of());
                        List<SimpleGrantedAuthority> authorities = roles.stream()
                                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.toUpperCase()))
                                .collect(Collectors.toList());

                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(subject, null, authorities);
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        log.debug("Authenticated user={} with roles={}", subject, roles);
                    }
                }
            } catch (Exception e) {
                log.debug("JWT processing error: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
