package io.authforge.service;

import io.authforge.domain.*;
import io.authforge.exception.AuthException;
import io.authforge.exception.ResourceNotFoundException;
import io.authforge.repository.AuthCodeRepository;
import io.authforge.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Core authentication service — implements all OAuth2/OIDC grant types.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final String GRANT_PASSWORD             = "password";
    private static final String GRANT_AUTH_CODE            = "authorization_code";
    private static final String GRANT_REFRESH_TOKEN        = "refresh_token";
    private static final String GRANT_CLIENT_CREDENTIALS   = "client_credentials";

    private final UserRepository userRepository;
    private final AuthCodeRepository authCodeRepository;
    private final RealmService realmService;
    private final ClientService clientService;
    private final UserService userService;
    private final TokenService tokenService;
    private final SessionService sessionService;
    private final AuditService auditService;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // -------------------------------------------------------------------------
    // Token endpoint dispatch
    // -------------------------------------------------------------------------

    @Transactional
    public Map<String, Object> token(String realmName,
                                     String grantType,
                                     Map<String, String> params,
                                     HttpServletRequest request) {
        Realm realm = realmService.findByName(realmName);
        if (!Boolean.TRUE.equals(realm.getEnabled())) {
            throw new AuthException("Realm is disabled");
        }

        return switch (grantType) {
            case GRANT_PASSWORD           -> passwordGrant(realm, params, request);
            case GRANT_AUTH_CODE          -> authorizationCodeGrant(realm, params, request);
            case GRANT_REFRESH_TOKEN      -> refreshTokenGrant(realm, params, request);
            case GRANT_CLIENT_CREDENTIALS -> clientCredentialsGrant(realm, params, request);
            default -> throw new AuthException("Unsupported grant_type: " + grantType);
        };
    }

    // -------------------------------------------------------------------------
    // Resource Owner Password Credentials grant
    // -------------------------------------------------------------------------

    private Map<String, Object> passwordGrant(Realm realm, Map<String, String> params, HttpServletRequest request) {
        String username    = require(params, "username");
        String password    = require(params, "password");
        String clientId    = require(params, "client_id");
        String clientSecret = params.get("client_secret");

        Client client = validateClient(realm, clientId, clientSecret);

        User user = userRepository.findByRealmIdAndUsername(realm.getId(), username)
                .orElseGet(() -> userRepository.findByRealmIdAndEmail(realm.getId(), username)
                        .orElseThrow(() -> new AuthException("Invalid credentials")));

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new AuthException("User account is disabled");
        }
        if (!userService.verifyPassword(user, password)) {
            auditFailedLogin(realm, user.getId(), clientId, request);
            throw new AuthException("Invalid credentials");
        }

        return issueTokens(realm, user, client, request);
    }

    // -------------------------------------------------------------------------
    // Authorization Code grant
    // -------------------------------------------------------------------------

    /**
     * Exchange an authorization code for tokens.
     */
    private Map<String, Object> authorizationCodeGrant(Realm realm, Map<String, String> params, HttpServletRequest request) {
        String code        = require(params, "code");
        String clientId    = require(params, "client_id");
        String redirectUri = require(params, "redirect_uri");
        String clientSecret = params.get("client_secret");

        Client client = validateClient(realm, clientId, clientSecret);

        AuthCode authCode = authCodeRepository.findByCodeAndUsedFalse(code)
                .orElseThrow(() -> new AuthException("Invalid or expired authorization code"));

        if (authCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AuthException("Authorization code has expired");
        }
        if (!authCode.getClientId().equals(clientId)) {
            throw new AuthException("Code was not issued to this client");
        }
        if (!authCode.getRealmId().equals(realm.getId())) {
            throw new AuthException("Code belongs to a different realm");
        }

        // Mark code as used (one-time use)
        authCode.setUsed(true);
        authCodeRepository.save(authCode);

        User user = userRepository.findById(authCode.getUserId())
                .orElseThrow(() -> new AuthException("User not found"));

        return issueTokens(realm, user, client, request);
    }

    /**
     * Create and store an authorization code for the given user/realm/client.
     * Called by the /auth endpoint after successful interactive login.
     */
    @Transactional
    public String createAuthorizationCode(UUID userId, UUID realmId, String clientId,
                                          String redirectUri, String scope) {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        AuthCode authCode = AuthCode.builder()
                .code(code)
                .userId(userId)
                .realmId(realmId)
                .clientId(clientId)
                .redirectUri(redirectUri)
                .scope(scope)
                .expiresAt(LocalDateTime.now().plusSeconds(300)) // 5-minute code TTL
                .used(false)
                .build();
        authCodeRepository.save(authCode);
        return code;
    }

    // -------------------------------------------------------------------------
    // Refresh Token grant
    // -------------------------------------------------------------------------

    private Map<String, Object> refreshTokenGrant(Realm realm, Map<String, String> params, HttpServletRequest request) {
        String refreshTokenValue = require(params, "refresh_token");
        String clientId          = require(params, "client_id");
        String clientSecret      = params.get("client_secret");

        validateClient(realm, clientId, clientSecret);

        Session session = sessionService.findByRefreshToken(refreshTokenValue);
        if (sessionService.isExpired(session)) {
            sessionService.revokeSession(session.getId());
            throw new AuthException("Refresh token has expired");
        }

        User user = userRepository.findById(session.getUserId())
                .orElseThrow(() -> new AuthException("User not found"));

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new AuthException("User account is disabled");
        }

        // Rotate: revoke old session, issue new tokens
        sessionService.revokeSession(session.getId());

        String newAccessToken  = tokenService.generateAccessToken(user, realm);
        String newRefreshToken = tokenService.generateRefreshToken(user, realm);

        sessionService.createSession(
                user.getId(), realm.getId(), clientId,
                getClientIp(request), getClientUserAgent(request),
                newRefreshToken, realm.getRefreshTokenLifespan());

        auditService.record("TOKEN_REFRESH", realm.getId(), user.getId(), clientId,
                getClientIp(request), Map.of("username", user.getUsername()));

        return tokenService.buildTokenResponse(newAccessToken, newRefreshToken, realm);
    }

    // -------------------------------------------------------------------------
    // Client Credentials grant
    // -------------------------------------------------------------------------

    private Map<String, Object> clientCredentialsGrant(Realm realm, Map<String, String> params, HttpServletRequest request) {
        String clientId     = require(params, "client_id");
        String clientSecret = require(params, "client_secret");

        Client client = validateClient(realm, clientId, clientSecret);
        if (Boolean.TRUE.equals(client.getPublicClient())) {
            throw new AuthException("Public clients cannot use client_credentials grant");
        }

        // Issue a machine-to-machine access token with no refresh token
        User syntheticUser = User.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000000"))
                .realmId(realm.getId())
                .username(clientId)
                .email(clientId + "@client")
                .passwordHash("")
                .build();

        String accessToken = tokenService.generateAccessToken(syntheticUser, realm);

        auditService.record("CLIENT_CREDENTIALS_GRANT", realm.getId(), null, clientId,
                getClientIp(request), Map.of());

        return Map.of(
                "access_token", accessToken,
                "token_type", "Bearer",
                "expires_in", realm.getAccessTokenLifespan()
        );
    }

    // -------------------------------------------------------------------------
    // Logout
    // -------------------------------------------------------------------------

    @Transactional
    public void logout(String realmName, String refreshToken, String accessToken, HttpServletRequest request) {
        try {
            Session session = sessionService.findByRefreshToken(refreshToken);
            User user = userRepository.findById(session.getUserId()).orElse(null);

            sessionService.revokeSession(session.getId());

            if (accessToken != null) {
                tokenService.revokeToken(accessToken);
            }

            if (user != null) {
                Realm realm = realmService.findByName(realmName);
                auditService.record("LOGOUT", realm.getId(), user.getId(), session.getClientId(),
                        getClientIp(request), Map.of("username", user.getUsername()));
            }
        } catch (ResourceNotFoundException e) {
            log.debug("Logout: session not found for refresh token");
        }
    }

    // -------------------------------------------------------------------------
    // Self-service registration
    // -------------------------------------------------------------------------

    @Transactional
    public User register(String realmName, String username, String email, String password,
                         String firstName, String lastName, HttpServletRequest request) {
        Realm realm = realmService.findByName(realmName);
        if (!Boolean.TRUE.equals(realm.getEnabled())) {
            throw new AuthException("Realm is disabled");
        }
        if (!Boolean.TRUE.equals(realm.getRegistrationAllowed())) {
            throw new AuthException("Registration is not allowed in this realm");
        }

        User newUser = User.builder()
                .username(username)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .enabled(true)
                .emailVerified(false)
                .build();

        User saved = userService.create(realmName, newUser, password);

        auditService.record("REGISTER", realm.getId(), saved.getId(), null,
                getClientIp(request), Map.of("username", username, "email", email));

        return saved;
    }

    // -------------------------------------------------------------------------
    // Userinfo endpoint
    // -------------------------------------------------------------------------

    public Map<String, Object> userinfo(String realmName, String token) {
        if (!tokenService.validateToken(token)) {
            throw new AuthException("Invalid or expired token");
        }

        Claims claims;
        try {
            claims = tokenService.parseToken(token);
        } catch (Exception e) {
            throw new AuthException("Cannot parse token");
        }

        String userId = claims.getSubject();
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new AuthException("User not found"));

        List<String> roles = user.getRoles().stream().map(Role::getName).toList();

        return Map.of(
                "sub",               user.getId().toString(),
                "preferred_username", user.getUsername(),
                "email",             user.getEmail(),
                "email_verified",    user.getEmailVerified(),
                "given_name",        user.getFirstName() != null ? user.getFirstName() : "",
                "family_name",       user.getLastName()  != null ? user.getLastName()  : "",
                "realm_access",      Map.of("roles", roles)
        );
    }

    // -------------------------------------------------------------------------
    // Token introspection
    // -------------------------------------------------------------------------

    public Map<String, Object> introspect(String realmName, String token) {
        boolean active = tokenService.validateToken(token);
        if (!active) {
            return Map.of("active", false);
        }
        try {
            Claims claims = tokenService.parseToken(token);
            return Map.of(
                    "active", true,
                    "sub",    claims.getSubject(),
                    "exp",    claims.getExpiration().getTime() / 1000,
                    "iat",    claims.getIssuedAt().getTime() / 1000,
                    "iss",    claims.getIssuer(),
                    "roles",  claims.getOrDefault("roles", List.of())
            );
        } catch (Exception e) {
            return Map.of("active", false);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Map<String, Object> issueTokens(Realm realm, User user, Client client, HttpServletRequest request) {
        String accessToken  = tokenService.generateAccessToken(user, realm);
        String refreshToken = tokenService.generateRefreshToken(user, realm);

        sessionService.createSession(
                user.getId(), realm.getId(), client.getClientId(),
                getClientIp(request), getClientUserAgent(request),
                refreshToken, realm.getRefreshTokenLifespan());

        // Update last login timestamp
        user.setLastLogin(java.time.LocalDateTime.now());
        userRepository.save(user);

        auditService.record("LOGIN_SUCCESS", realm.getId(), user.getId(), client.getClientId(),
                getClientIp(request), Map.of("username", user.getUsername()));

        return tokenService.buildTokenResponse(accessToken, refreshToken, realm);
    }

    private Client validateClient(Realm realm, String clientId, String clientSecret) {
        Client client = clientService.findByRealmAndClientId(realm.getName(), clientId);
        if (!Boolean.TRUE.equals(client.getEnabled())) {
            throw new AuthException("Client is disabled");
        }
        if (!Boolean.TRUE.equals(client.getPublicClient())) {
            if (clientSecret == null || !clientService.verifyClientSecret(client, clientSecret)) {
                throw new AuthException("Invalid client credentials");
            }
        }
        return client;
    }

    private void auditFailedLogin(Realm realm, UUID userId, String clientId, HttpServletRequest request) {
        auditService.record("LOGIN_FAILED", realm.getId(), userId, clientId,
                getClientIp(request), Map.of());
    }

    private String require(Map<String, String> params, String key) {
        String value = params.get(key);
        if (value == null || value.isBlank()) {
            throw new AuthException("Missing required parameter: " + key);
        }
        return value;
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        return (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim() : request.getRemoteAddr();
    }

    private String getClientUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    // Expose parseToken for controllers that need Claims
    public Claims parseToken(String token) {
        return tokenService.parseToken(token);
    }
}
