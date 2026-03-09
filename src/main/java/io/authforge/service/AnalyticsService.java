package io.authforge.service;

import io.authforge.repository.AuditLogRepository;
import io.authforge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final SessionService sessionService;

    /**
     * Return a high-level overview for the admin dashboard.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getOverview(UUID realmId) {
        LocalDateTime last24h = LocalDateTime.now().minusHours(24);
        LocalDateTime last7d  = LocalDateTime.now().minusDays(7);

        long totalUsers         = userRepository.countByRealmId(realmId);
        long activeSessions     = sessionService.countActiveSessions(realmId);
        long loginsToday        = auditLogRepository.countByRealmIdAndEventTypeSince(realmId, "LOGIN_SUCCESS", last24h);
        long failedLoginsToday  = auditLogRepository.countByRealmIdAndEventTypeSince(realmId, "LOGIN_FAILED", last24h);
        long uniqueLoginsWeek   = auditLogRepository.countUniqueLoginsSince(realmId, last7d);

        return Map.of(
                "totalUsers",        totalUsers,
                "activeSessions",    activeSessions,
                "loginsToday",       loginsToday,
                "failedLoginsToday", failedLoginsToday,
                "uniqueLoginsThisWeek", uniqueLoginsWeek
        );
    }

    /**
     * Return login counts over the last N days (one entry per day).
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getLoginStats(UUID realmId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        long success = auditLogRepository.countByRealmIdAndEventTypeSince(realmId, "LOGIN_SUCCESS", since);
        long failed  = auditLogRepository.countByRealmIdAndEventTypeSince(realmId, "LOGIN_FAILED", since);
        return Map.of(
                "period", days + "d",
                "successfulLogins", success,
                "failedLogins", failed
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getActiveSessionStats(UUID realmId) {
        return Map.of("activeSessions", sessionService.countActiveSessions(realmId));
    }
}
