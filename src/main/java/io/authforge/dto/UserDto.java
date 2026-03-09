package io.authforge.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
public class UserDto {
    private UUID id;
    private UUID realmId;

    @NotBlank
    @Size(min = 3, max = 150)
    private String username;

    @Email
    @NotBlank
    private String email;

    private String password;
    private String firstName;
    private String lastName;
    private Boolean enabled;
    private Boolean emailVerified;
    private Set<String> roles;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
}
