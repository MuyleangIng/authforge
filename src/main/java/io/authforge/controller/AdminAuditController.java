package io.authforge.controller;

import io.authforge.domain.AuditLog;
import io.authforge.dto.AuditLogDto;
import io.authforge.service.AuditService;
import io.authforge.service.RealmService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin/realms/{realm}/audit-log")
@RequiredArgsConstructor
@PreAuthorize("hasRole('admin')")
public class AdminAuditController {

    private final AuditService auditService;
    private final RealmService realmService;

    @GetMapping
    public ResponseEntity<Page<AuditLogDto>> getAuditLog(@PathVariable String realm, Pageable pageable) {
        UUID realmId = realmService.findByName(realm).getId();
        Page<AuditLogDto> page = auditService.findByRealm(realmId, pageable).map(this::toDto);
        return ResponseEntity.ok(page);
    }

    private AuditLogDto toDto(AuditLog log) {
        return AuditLogDto.builder()
                .id(log.getId())
                .realmId(log.getRealmId())
                .userId(log.getUserId())
                .eventType(log.getEventType())
                .clientId(log.getClientId())
                .ipAddress(log.getIpAddress())
                .details(log.getDetails())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
