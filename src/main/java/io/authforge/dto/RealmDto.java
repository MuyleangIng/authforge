package io.authforge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class RealmDto {
    private UUID id;

    @NotBlank
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Realm name must be lowercase alphanumeric with hyphens")
    private String name;

    private String displayName;
    private Boolean enabled;
    private Boolean registrationAllowed;
    private Integer accessTokenLifespan;
    private Integer refreshTokenLifespan;
    private Integer ssoSessionIdle;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
