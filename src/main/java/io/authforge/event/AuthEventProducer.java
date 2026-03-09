package io.authforge.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Publishes {@link AuthEvent} records to the appropriate Kafka topic.
 *
 * <p>Topic naming convention: {@code authforge.<category>.<action>}
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthEventProducer {

    // Topic constants
    public static final String TOPIC_AUTH_LOGIN           = "authforge.auth.login";
    public static final String TOPIC_AUTH_LOGIN_FAILED    = "authforge.auth.login_failed";
    public static final String TOPIC_AUTH_LOGOUT          = "authforge.auth.logout";
    public static final String TOPIC_AUTH_REGISTER        = "authforge.auth.register";
    public static final String TOPIC_TOKEN_REFRESH        = "authforge.token.refresh";
    public static final String TOPIC_SESSION_EXPIRED      = "authforge.session.expired";
    public static final String TOPIC_ADMIN_USER_CREATED   = "authforge.admin.user.created";
    public static final String TOPIC_ADMIN_USER_UPDATED   = "authforge.admin.user.updated";
    public static final String TOPIC_ADMIN_USER_DELETED   = "authforge.admin.user.deleted";

    private final KafkaTemplate<String, AuthEvent> kafkaTemplate;

    /**
     * Publish an event to the topic that corresponds to its {@code eventType}.
     * The message key is {@code realmId:userId} for ordered partitioning by user.
     */
    public void publish(AuthEvent event) {
        String topic = resolveTopic(event.eventType());
        String key   = buildKey(event);

        CompletableFuture<SendResult<String, AuthEvent>> future = kafkaTemplate.send(topic, key, event);
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.warn("Failed to publish event type={} to topic={}: {}", event.eventType(), topic, ex.getMessage());
            } else {
                log.debug("Published event type={} to topic={} partition={} offset={}",
                        event.eventType(), topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }

    private String resolveTopic(String eventType) {
        return switch (eventType) {
            case "LOGIN_SUCCESS"             -> TOPIC_AUTH_LOGIN;
            case "LOGIN_FAILED"              -> TOPIC_AUTH_LOGIN_FAILED;
            case "LOGOUT"                    -> TOPIC_AUTH_LOGOUT;
            case "REGISTER"                  -> TOPIC_AUTH_REGISTER;
            case "TOKEN_REFRESH"             -> TOPIC_TOKEN_REFRESH;
            case "SESSION_EXPIRED"           -> TOPIC_SESSION_EXPIRED;
            case "ADMIN_USER_CREATED"        -> TOPIC_ADMIN_USER_CREATED;
            case "ADMIN_USER_UPDATED"        -> TOPIC_ADMIN_USER_UPDATED;
            case "ADMIN_USER_DELETED"        -> TOPIC_ADMIN_USER_DELETED;
            default -> "authforge.misc";
        };
    }

    private String buildKey(AuthEvent event) {
        String realm  = event.realmId()  != null ? event.realmId()  : "unknown";
        String userId = event.userId()   != null ? event.userId()   : "system";
        return realm + ":" + userId;
    }
}
