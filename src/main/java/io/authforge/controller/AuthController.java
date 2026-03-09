package io.authforge.controller;

import io.authforge.domain.Realm;
import io.authforge.dto.RegisterRequest;
import io.authforge.service.AuthService;
import io.authforge.service.RealmService;
import io.authforge.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Implements the OpenID Connect / OAuth2 protocol endpoints for each realm.
 */
@RestController
@RequestMapping("/realms/{realm}")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RealmService realmService;
    private final TokenService tokenService;

    // -------------------------------------------------------------------------
    // OIDC Discovery
    // -------------------------------------------------------------------------

    @GetMapping("/.well-known/openid-configuration")
    public ResponseEntity<Map<String, Object>> discovery(@PathVariable String realm) {
        String base = "http://localhost:8080/realms/" + realm;
        Map<String, Object> discovery = new java.util.LinkedHashMap<>();
        discovery.put("issuer",                                base);
        discovery.put("authorization_endpoint",                base + "/protocol/openid-connect/auth");
        discovery.put("token_endpoint",                        base + "/protocol/openid-connect/token");
        discovery.put("userinfo_endpoint",                     base + "/protocol/openid-connect/userinfo");
        discovery.put("end_session_endpoint",                  base + "/protocol/openid-connect/logout");
        discovery.put("introspection_endpoint",                base + "/protocol/openid-connect/token/introspect");
        discovery.put("jwks_uri",                              base + "/protocol/openid-connect/certs");
        discovery.put("response_types_supported",              List.of("code", "token"));
        discovery.put("grant_types_supported",                 List.of("authorization_code", "password", "refresh_token", "client_credentials"));
        discovery.put("subject_types_supported",               List.of("public"));
        discovery.put("id_token_signing_alg_values_supported", List.of("HS256"));
        discovery.put("scopes_supported",                      List.of("openid", "profile", "email", "roles"));
        return ResponseEntity.ok(discovery);
    }

    @GetMapping("/protocol/openid-connect/certs")
    public ResponseEntity<Map<String, Object>> certs(@PathVariable String realm) {
        // JWKS endpoint — for HS256 symmetric keys there are no public keys to expose.
        // Return an empty keyset; clients should use the introspection endpoint.
        return ResponseEntity.ok(Map.of("keys", List.of()));
    }

    // -------------------------------------------------------------------------
    // Authorization endpoint (for auth-code flow — renders login UI)
    // -------------------------------------------------------------------------

    @GetMapping("/protocol/openid-connect/auth")
    public ResponseEntity<?> authorize(
            @PathVariable String realm,
            @RequestParam String client_id,
            @RequestParam String redirect_uri,
            @RequestParam(defaultValue = "code") String response_type,
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) String state) {
        // Redirect to the hosted login page
        String loginUrl = String.format("/login?realm=%s&client_id=%s&redirect_uri=%s&scope=%s&state=%s",
                realm, client_id, redirect_uri,
                scope != null ? scope : "openid",
                state != null ? state : "");
        return ResponseEntity.status(302)
                .header("Location", loginUrl)
                .build();
    }

    // -------------------------------------------------------------------------
    // Token endpoint
    // -------------------------------------------------------------------------

    @PostMapping(value = "/protocol/openid-connect/token",
                 consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Map<String, Object>> token(
            @PathVariable String realm,
            @RequestParam MultiValueMap<String, String> form,
            HttpServletRequest request) {

        String grantType = form.getFirst("grant_type");
        if (grantType == null || grantType.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "missing grant_type"));
        }

        // Flatten multi-value map to single-value map for service layer
        Map<String, String> params = new java.util.HashMap<>();
        form.forEach((k, v) -> params.put(k, v.isEmpty() ? null : v.get(0)));

        Map<String, Object> response = authService.token(realm, grantType, params, request);
        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------------
    // Logout endpoint
    // -------------------------------------------------------------------------

    @PostMapping(value = "/protocol/openid-connect/logout",
                 consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> logout(
            @PathVariable String realm,
            @RequestParam(value = "refresh_token", required = false) String refreshToken,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String accessToken = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        }

        if (refreshToken != null) {
            authService.logout(realm, refreshToken, accessToken, request);
        }

        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Userinfo endpoint
    // -------------------------------------------------------------------------

    @GetMapping("/protocol/openid-connect/userinfo")
    public ResponseEntity<Map<String, Object>> userinfo(
            @PathVariable String realm,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        return ResponseEntity.ok(authService.userinfo(realm, token));
    }

    // -------------------------------------------------------------------------
    // Token introspection
    // -------------------------------------------------------------------------

    @PostMapping(value = "/protocol/openid-connect/token/introspect",
                 consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Map<String, Object>> introspect(
            @PathVariable String realm,
            @RequestParam String token) {
        return ResponseEntity.ok(authService.introspect(realm, token));
    }

    // -------------------------------------------------------------------------
    // Self-service registration
    // -------------------------------------------------------------------------

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @PathVariable String realm,
            @Valid @RequestBody RegisterRequest req,
            HttpServletRequest request) {
        authService.register(realm,
                req.username(), req.email(), req.password(),
                req.firstName(), req.lastName(), request);
        return ResponseEntity.status(201).body(Map.of("message", "User registered successfully"));
    }
}
