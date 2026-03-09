package io.authforge.controller;

import io.authforge.domain.Realm;
import io.authforge.domain.Role;
import io.authforge.domain.Session;
import io.authforge.domain.User;
import io.authforge.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('admin')")
public class GraphQLController {

    private final RealmService realmService;
    private final UserService userService;
    private final RoleService roleService;
    private final SessionService sessionService;
    private final AnalyticsService analyticsService;

    // ---- Queries ----

    @QueryMapping
    public Map<String, Object> realm(@Argument String name) {
        return toRealmMap(realmService.findByName(name));
    }

    @QueryMapping
    public List<Map<String, Object>> realms() {
        return realmService.findAll().stream().map(this::toRealmMap).collect(Collectors.toList());
    }

    @QueryMapping
    public Map<String, Object> user(@Argument String realmName, @Argument String id) {
        return toUserMap(userService.findById(UUID.fromString(id)));
    }

    @QueryMapping
    public List<Map<String, Object>> users(@Argument String realmName) {
        return userService.findByRealm(realmName).stream().map(this::toUserMap).collect(Collectors.toList());
    }

    @QueryMapping
    public List<Map<String, Object>> roles(@Argument String realmName) {
        return roleService.findByRealm(realmName).stream().map(this::toRoleMap).collect(Collectors.toList());
    }

    @QueryMapping
    public List<Map<String, Object>> sessions(@Argument String realmName) {
        return sessionService.findByRealm(realmName).stream().map(this::toSessionMap).collect(Collectors.toList());
    }

    @QueryMapping
    public Map<String, Object> analyticsOverview(@Argument String realmName) {
        UUID realmId = realmService.findByName(realmName).getId();
        return analyticsService.getOverview(realmId);
    }

    // ---- Mutations ----

    @MutationMapping
    public Map<String, Object> createRealm(@Argument String name, @Argument String displayName) {
        Realm realm = Realm.builder().name(name).displayName(displayName).build();
        return toRealmMap(realmService.create(realm));
    }

    @MutationMapping
    public Boolean deleteRealm(@Argument String name) {
        Realm existing = realmService.findByName(name);
        realmService.delete(existing.getId());
        return true;
    }

    @MutationMapping
    public Map<String, Object> createUser(@Argument String realmName, @Argument String username,
                                           @Argument String email, @Argument String password) {
        User user = User.builder()
                .username(username)
                .email(email)
                .enabled(true)
                .emailVerified(false)
                .passwordHash("") // set by service
                .build();
        return toUserMap(userService.create(realmName, user, password));
    }

    @MutationMapping
    public Boolean deleteUser(@Argument String realmName, @Argument String id) {
        userService.delete(UUID.fromString(id));
        return true;
    }

    @MutationMapping
    public Map<String, Object> createRole(@Argument String realmName, @Argument String name,
                                           @Argument String description) {
        Role role = Role.builder().name(name).description(description).build();
        return toRoleMap(roleService.create(realmName, role));
    }

    @MutationMapping
    public Boolean deleteRole(@Argument String realmName, @Argument String id) {
        roleService.delete(UUID.fromString(id));
        return true;
    }

    @MutationMapping
    public Boolean assignRole(@Argument String realmName, @Argument String userId, @Argument String roleId) {
        userService.assignRole(UUID.fromString(userId), UUID.fromString(roleId));
        return true;
    }

    @MutationMapping
    public Boolean revokeSession(@Argument String realmName, @Argument String id) {
        sessionService.revokeSession(UUID.fromString(id));
        return true;
    }

    // ---- Mappers ----

    private Map<String, Object> toRealmMap(Realm r) {
        return Map.of(
                "id", r.getId().toString(),
                "name", r.getName(),
                "displayName", r.getDisplayName() != null ? r.getDisplayName() : "",
                "enabled", r.getEnabled() != null ? r.getEnabled() : true,
                "registrationAllowed", r.getRegistrationAllowed() != null ? r.getRegistrationAllowed() : true,
                "accessTokenLifespan", r.getAccessTokenLifespan() != null ? r.getAccessTokenLifespan() : 300,
                "refreshTokenLifespan", r.getRefreshTokenLifespan() != null ? r.getRefreshTokenLifespan() : 1800,
                "createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : ""
        );
    }

    private Map<String, Object> toUserMap(User u) {
        Set<String> roles = u.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        return Map.of(
                "id", u.getId().toString(),
                "username", u.getUsername(),
                "email", u.getEmail(),
                "firstName", u.getFirstName() != null ? u.getFirstName() : "",
                "lastName", u.getLastName() != null ? u.getLastName() : "",
                "enabled", u.getEnabled() != null ? u.getEnabled() : true,
                "emailVerified", u.getEmailVerified() != null ? u.getEmailVerified() : false,
                "roles", List.copyOf(roles),
                "createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : ""
        );
    }

    private Map<String, Object> toRoleMap(Role r) {
        return Map.of(
                "id", r.getId().toString(),
                "name", r.getName(),
                "description", r.getDescription() != null ? r.getDescription() : "",
                "isComposite", r.getIsComposite() != null ? r.getIsComposite() : false,
                "clientRole", r.getClientRole() != null ? r.getClientRole() : false
        );
    }

    private Map<String, Object> toSessionMap(Session s) {
        return Map.of(
                "id", s.getId().toString(),
                "userId", s.getUserId().toString(),
                "clientId", s.getClientId() != null ? s.getClientId() : "",
                "ipAddress", s.getIpAddress() != null ? s.getIpAddress() : "",
                "expiresAt", s.getExpiresAt() != null ? s.getExpiresAt().toString() : "",
                "createdAt", s.getCreatedAt() != null ? s.getCreatedAt().toString() : ""
        );
    }
}
