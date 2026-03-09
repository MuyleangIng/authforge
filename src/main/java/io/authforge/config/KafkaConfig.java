package io.authforge.config;

import io.authforge.event.AuthEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class KafkaConfig {

    // Create all topics on startup (auto-create is also enabled in docker-compose,
    // but explicit declarations ensure they exist before producers try to send)

    @Bean public NewTopic topicAuthLogin()         { return topic("authforge.auth.login"); }
    @Bean public NewTopic topicAuthLoginFailed()   { return topic("authforge.auth.login_failed"); }
    @Bean public NewTopic topicAuthLogout()        { return topic("authforge.auth.logout"); }
    @Bean public NewTopic topicAuthRegister()      { return topic("authforge.auth.register"); }
    @Bean public NewTopic topicTokenRefresh()      { return topic("authforge.token.refresh"); }
    @Bean public NewTopic topicSessionExpired()    { return topic("authforge.session.expired"); }
    @Bean public NewTopic topicAdminUserCreated()  { return topic("authforge.admin.user.created"); }
    @Bean public NewTopic topicAdminUserUpdated()  { return topic("authforge.admin.user.updated"); }
    @Bean public NewTopic topicAdminUserDeleted()  { return topic("authforge.admin.user.deleted"); }
    @Bean public NewTopic topicMisc()              { return topic("authforge.misc"); }

    @Bean
    public KafkaTemplate<String, AuthEvent> authEventKafkaTemplate(
            ProducerFactory<String, AuthEvent> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    private NewTopic topic(String name) {
        return TopicBuilder.name(name)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
