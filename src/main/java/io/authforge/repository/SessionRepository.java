package io.authforge.repository;

import io.authforge.domain.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, UUID> {
    Optional<Session> findByRefreshToken(String refreshToken);
    List<Session> findByUserId(UUID userId);
    List<Session> findByRealmId(UUID realmId);
    List<Session> findByUserIdAndRealmId(UUID userId, UUID realmId);

    @Modifying
    @Query("DELETE FROM Session s WHERE s.expiresAt < :now")
    int deleteExpiredSessions(LocalDateTime now);

    long countByRealmIdAndExpiresAtAfter(UUID realmId, LocalDateTime now);
}
