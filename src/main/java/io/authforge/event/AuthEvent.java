package io.authforge.event;

import java.time.Instant;
import java.util.Map;

/**
 * Immutable event record published to Kafka for every significant auth action.
 */
public record AuthEvent(
        String eventType,
        String realmId,
        String userId,
        String clientId,
        String ipAddress,
        Instant timestamp,
        Map<String, Object> details
) {}
