package io.authforge.service;

import io.authforge.domain.AuditLog;
import io.authforge.event.AuthEvent;
import io.authforge.event.AuthEventProducer;
import io.authforge.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final AuthEventProducer eventProducer;

    /**
     * Record an audit event to the database and publish it to Kafka.
     */
    @Transactional
    public void record(String eventType,
                       UUID realmId,
                       UUID userId,
                       String clientId,
                       String ipAddress,
                       Map<String, Object> details) {

        // Persist to database
        AuditLog entry = AuditLog.builder()
                .eventType(eventType)
                .realmId(realmId)
                .userId(userId)
                .clientId(clientId)
                .ipAddress(ipAddress)
                .details(details)
                .build();
        auditLogRepository.save(entry);

        // Publish to Kafka (fire-and-forget — do not block the caller)
        try {
            AuthEvent event = new AuthEvent(
                    eventType,
                    realmId != null ? realmId.toString() : null,
                    userId != null ? userId.toString() : null,
                    clientId,
                    ipAddress,
                    Instant.now(),
                    details != null ? details : Map.of()
            );
            eventProducer.publish(event);
        } catch (Exception e) {
            // Kafka failure must not roll back the audit DB write
            log.warn("Failed to publish audit event to Kafka: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> findByRealm(UUID realmId, Pageable pageable) {
        return auditLogRepository.findByRealmIdOrderByCreatedAtDesc(realmId, pageable);
    }
}
