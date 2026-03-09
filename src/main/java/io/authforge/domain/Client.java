package io.authforge.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "clients")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "realm_id", nullable = false)
    private UUID realmId;

    @Column(name = "client_id", nullable = false, length = 100)
    private String clientId;

    @Column(name = "client_secret", length = 255)
    private String clientSecret;

    @Column(length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    private Boolean enabled = true;

    @Builder.Default
    @Column(name = "public_client")
    private Boolean publicClient = false;

    @Builder.Default
    @Column(name = "redirect_uris", columnDefinition = "TEXT")
    private String redirectUris = "[]";

    @Builder.Default
    @Column(name = "web_origins", columnDefinition = "TEXT")
    private String webOrigins = "[]";

    @Builder.Default
    @Column(name = "grant_types", columnDefinition = "TEXT")
    private String grantTypes = "[\"authorization_code\",\"refresh_token\"]";

    @Builder.Default
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
