package io.authforge.controller;

import io.authforge.domain.Role;
import io.authforge.domain.Session;
import io.authforge.domain.User;
import io.authforge.dto.RoleDto;
import io.authforge.dto.SessionDto;
import io.authforge.dto.UserDto;
import io.authforge.service.SessionService;
import io.authforge.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/realms/{realm}/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('admin')")
public class AdminUserController {

    private final UserService userService;
    private final SessionService sessionService;

    @GetMapping
    public ResponseEntity<List<UserDto>> getUsers(@PathVariable String realm) {
        List<UserDto> users = userService.findByRealm(realm).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @PostMapping
    public ResponseEntity<UserDto> createUser(@PathVariable String realm, @Valid @RequestBody UserDto dto) {
        User user = User.builder()
                .username(dto.getUsername())
                .email(dto.getEmail())
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .enabled(dto.getEnabled() != null ? dto.getEnabled() : true)
                .emailVerified(dto.getEmailVerified() != null ? dto.getEmailVerified() : false)
                .passwordHash("") // will be set by service
                .build();
        String password = dto.getPassword() != null ? dto.getPassword() : UUID.randomUUID().toString();
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(userService.create(realm, user, password)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUser(@PathVariable String realm, @PathVariable UUID id) {
        return ResponseEntity.ok(toDto(userService.findById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(@PathVariable String realm, @PathVariable UUID id,
                                               @RequestBody UserDto dto) {
        User patch = User.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .enabled(dto.getEnabled())
                .emailVerified(dto.getEmailVerified())
                .passwordHash(null) // not updated here
                .build();
        User updated = userService.update(id, patch);
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            userService.changePassword(id, dto.getPassword());
        }
        return ResponseEntity.ok(toDto(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String realm, @PathVariable UUID id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/roles")
    public ResponseEntity<Set<RoleDto>> getUserRoles(@PathVariable String realm, @PathVariable UUID id) {
        Set<RoleDto> roles = userService.findById(id).getRoles().stream()
                .map(this::toRoleDto)
                .collect(Collectors.toSet());
        return ResponseEntity.ok(roles);
    }

    @PostMapping("/{id}/roles")
    public ResponseEntity<Void> assignRoles(@PathVariable String realm, @PathVariable UUID id,
                                             @RequestBody Set<UUID> roleIds) {
        for (UUID roleId : roleIds) {
            userService.assignRole(id, roleId);
        }
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/roles/{roleId}")
    public ResponseEntity<Void> removeRole(@PathVariable String realm, @PathVariable UUID id,
                                            @PathVariable UUID roleId) {
        userService.removeRole(id, roleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/sessions")
    public ResponseEntity<List<SessionDto>> getUserSessions(@PathVariable String realm, @PathVariable UUID id) {
        List<SessionDto> sessions = sessionService.findByUser(id).stream()
                .map(this::toSessionDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(sessions);
    }

    @DeleteMapping("/{id}/sessions")
    public ResponseEntity<Void> revokeUserSessions(@PathVariable String realm, @PathVariable UUID id) {
        sessionService.revokeAllUserSessions(id);
        return ResponseEntity.noContent().build();
    }

    private UserDto toDto(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
        return UserDto.builder()
                .id(user.getId())
                .realmId(user.getRealmId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .enabled(user.getEnabled())
                .emailVerified(user.getEmailVerified())
                .roles(roleNames)
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private RoleDto toRoleDto(Role role) {
        return RoleDto.builder()
                .id(role.getId())
                .realmId(role.getRealmId())
                .name(role.getName())
                .description(role.getDescription())
                .isComposite(role.getIsComposite())
                .clientRole(role.getClientRole())
                .createdAt(role.getCreatedAt())
                .build();
    }

    private SessionDto toSessionDto(Session session) {
        return SessionDto.builder()
                .id(session.getId())
                .userId(session.getUserId())
                .realmId(session.getRealmId())
                .clientId(session.getClientId())
                .ipAddress(session.getIpAddress())
                .userAgent(session.getUserAgent())
                .expiresAt(session.getExpiresAt())
                .createdAt(session.getCreatedAt())
                .lastAccessed(session.getLastAccessed())
                .build();
    }
}
