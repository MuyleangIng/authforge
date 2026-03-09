package io.authforge.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class AuditLogDto {
    private UUID id;
    private UUID realmId;
    private UUID userId;
    private String eventType;
    private String clientId;
    private String ipAddress;
    private Map<String, Object> details;
    private LocalDateTime createdAt;
}
