package io.authforge.controller;

import io.authforge.domain.Session;
import io.authforge.dto.SessionDto;
import io.authforge.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/realms/{realm}/sessions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('admin')")
public class AdminSessionController {

    private final SessionService sessionService;

    @GetMapping
    public ResponseEntity<List<SessionDto>> getSessions(@PathVariable String realm) {
        List<SessionDto> sessions = sessionService.findByRealm(realm).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(sessions);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSession(@PathVariable String realm, @PathVariable UUID id) {
        sessionService.revokeSession(id);
        return ResponseEntity.noContent().build();
    }

    private SessionDto toDto(Session session) {
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
