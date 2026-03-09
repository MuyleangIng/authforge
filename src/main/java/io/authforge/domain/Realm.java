package io.authforge.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "realms")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Realm {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 100)
    private String name;

    @Column(name = "display_name", length = 200)
    private String displayName;

    @Builder.Default
    private Boolean enabled = true;

    @Builder.Default
    @Column(name = "registration_allowed")
    private Boolean registrationAllowed = true;

    @Builder.Default
    @Column(name = "access_token_lifespan")
    private Integer accessTokenLifespan = 300;

    @Builder.Default
    @Column(name = "refresh_token_lifespan")
    private Integer refreshTokenLifespan = 1800;

    @Builder.Default
    @Column(name = "sso_session_idle")
    private Integer ssoSessionIdle = 1800;

    @Builder.Default
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
