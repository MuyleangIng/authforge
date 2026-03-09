package io.authforge.repository;

import io.authforge.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    Page<AuditLog> findByRealmIdOrderByCreatedAtDesc(UUID realmId, Pageable pageable);
    List<AuditLog> findByRealmIdAndEventTypeOrderByCreatedAtDesc(UUID realmId, String eventType, Pageable pageable);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.realmId = :realmId AND a.eventType = :eventType AND a.createdAt >= :since")
    long countByRealmIdAndEventTypeSince(UUID realmId, String eventType, LocalDateTime since);

    @Query("SELECT COUNT(DISTINCT a.userId) FROM AuditLog a WHERE a.realmId = :realmId AND a.eventType = 'LOGIN_SUCCESS' AND a.createdAt >= :since")
    long countUniqueLoginsSince(UUID realmId, LocalDateTime since);
}
