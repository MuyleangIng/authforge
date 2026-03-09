package io.authforge.service;

import io.authforge.domain.Session;
import io.authforge.exception.ResourceNotFoundException;
import io.authforge.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    private final SessionRepository sessionRepository;
    private final RealmService realmService;

    @Transactional
    public Session createSession(UUID userId, UUID realmId, String clientId,
                                  String ipAddress, String userAgent,
                                  String refreshToken, int lifespanSeconds) {
        Session session = Session.builder()
                .userId(userId)
                .realmId(realmId)
                .clientId(clientId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .refreshToken(refreshToken)
                .expiresAt(LocalDateTime.now().plusSeconds(lifespanSeconds))
                .build();
        return sessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public Session findById(UUID id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session", "id", id));
    }

    @Transactional(readOnly = true)
    public Session findByRefreshToken(String refreshToken) {
        return sessionRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new ResourceNotFoundException("Session", "refreshToken", "***"));
    }

    @Transactional(readOnly = true)
    public List<Session> findByUser(UUID userId) {
        return sessionRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<Session> findByRealm(String realmName) {
        UUID realmId = realmService.findByName(realmName).getId();
        return sessionRepository.findByRealmId(realmId);
    }

    @Transactional
    public void revokeSession(UUID sessionId) {
        sessionRepository.deleteById(sessionId);
        log.debug("Revoked session: {}", sessionId);
    }

    @Transactional
    public void revokeAllUserSessions(UUID userId) {
        List<Session> sessions = sessionRepository.findByUserId(userId);
        sessionRepository.deleteAll(sessions);
        log.info("Revoked {} sessions for user {}", sessions.size(), userId);
    }

    @Transactional
    public void touchSession(UUID sessionId) {
        sessionRepository.findById(sessionId).ifPresent(s -> {
            s.setLastAccessed(LocalDateTime.now());
            sessionRepository.save(s);
        });
    }

    public boolean isExpired(Session session) {
        return session.getExpiresAt().isBefore(LocalDateTime.now());
    }

    /**
     * Scheduled cleanup: remove expired sessions every 5 minutes.
     */
    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void purgeExpiredSessions() {
        int deleted = sessionRepository.deleteExpiredSessions(LocalDateTime.now());
        if (deleted > 0) {
            log.info("Purged {} expired sessions", deleted);
        }
    }

    public long countActiveSessions(UUID realmId) {
        return sessionRepository.countByRealmIdAndExpiresAtAfter(realmId, LocalDateTime.now());
    }
}
