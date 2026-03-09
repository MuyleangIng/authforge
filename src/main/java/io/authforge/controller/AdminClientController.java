package io.authforge.controller;

import io.authforge.domain.Client;
import io.authforge.dto.ClientDto;
import io.authforge.service.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/realms/{realm}/clients")
@RequiredArgsConstructor
@PreAuthorize("hasRole('admin')")
public class AdminClientController {

    private final ClientService clientService;

    @GetMapping
    public ResponseEntity<List<ClientDto>> getClients(@PathVariable String realm) {
        List<ClientDto> clients = clientService.findByRealm(realm).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(clients);
    }

    @PostMapping
    public ResponseEntity<ClientDto> createClient(@PathVariable String realm, @Valid @RequestBody ClientDto dto) {
        Client client = Client.builder()
                .clientId(dto.getClientId())
                .name(dto.getName())
                .description(dto.getDescription())
                .enabled(dto.getEnabled() != null ? dto.getEnabled() : true)
                .publicClient(dto.getPublicClient() != null ? dto.getPublicClient() : false)
                .redirectUris(dto.getRedirectUris() != null ? dto.getRedirectUris() : "[]")
                .webOrigins(dto.getWebOrigins() != null ? dto.getWebOrigins() : "[]")
                .grantTypes(dto.getGrantTypes() != null ? dto.getGrantTypes() : "[\"authorization_code\",\"refresh_token\"]")
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(clientService.create(realm, client)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientDto> getClient(@PathVariable String realm, @PathVariable UUID id) {
        return ResponseEntity.ok(toDto(clientService.findById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClientDto> updateClient(@PathVariable String realm, @PathVariable UUID id,
                                                   @RequestBody ClientDto dto) {
        Client patch = Client.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .enabled(dto.getEnabled())
                .redirectUris(dto.getRedirectUris())
                .webOrigins(dto.getWebOrigins())
                .grantTypes(dto.getGrantTypes())
                .build();
        return ResponseEntity.ok(toDto(clientService.update(id, patch)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClient(@PathVariable String realm, @PathVariable UUID id) {
        clientService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/regenerate-secret")
    public ResponseEntity<ClientDto> regenerateSecret(@PathVariable String realm, @PathVariable UUID id) {
        String newSecret = clientService.regenerateSecret(id);
        ClientDto dto = toDto(clientService.findById(id));
        dto.setClientSecret(newSecret);
        return ResponseEntity.ok(dto);
    }

    private ClientDto toDto(Client client) {
        return ClientDto.builder()
                .id(client.getId())
                .realmId(client.getRealmId())
                .clientId(client.getClientId())
                .name(client.getName())
                .description(client.getDescription())
                .enabled(client.getEnabled())
                .publicClient(client.getPublicClient())
                .redirectUris(client.getRedirectUris())
                .webOrigins(client.getWebOrigins())
                .grantTypes(client.getGrantTypes())
                .createdAt(client.getCreatedAt())
                .build();
    }
}
