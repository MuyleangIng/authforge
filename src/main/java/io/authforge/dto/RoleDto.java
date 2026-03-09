package io.authforge.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class RoleDto {
    private UUID id;
    private UUID realmId;

    @NotBlank
    private String name;

    private String description;
    private Boolean isComposite;
    private Boolean clientRole;
    private LocalDateTime createdAt;
}
