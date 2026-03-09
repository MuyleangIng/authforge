package io.authforge.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ClientDto {
    private UUID id;
    private UUID realmId;

    @NotBlank
    private String clientId;

    private String clientSecret;
    private String name;
    private String description;
    private Boolean enabled;
    private Boolean publicClient;
    private String redirectUris;
    private String webOrigins;
    private String grantTypes;
    private LocalDateTime createdAt;
}
