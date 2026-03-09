package io.authforge.controller;

import io.authforge.domain.Realm;
import io.authforge.dto.RealmDto;
import io.authforge.service.RealmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/realms")
@RequiredArgsConstructor
@PreAuthorize("hasRole('admin')")
public class AdminRealmController {

    private final RealmService realmService;

    @GetMapping
    public ResponseEntity<List<RealmDto>> getAllRealms() {
        List<RealmDto> realms = realmService.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(realms);
    }

    @PostMapping
    public ResponseEntity<RealmDto> createRealm(@Valid @RequestBody RealmDto dto) {
        Realm realm = Realm.builder()
                .name(dto.getName())
                .displayName(dto.getDisplayName())
                .enabled(dto.getEnabled() != null ? dto.getEnabled() : true)
                .registrationAllowed(dto.getRegistrationAllowed() != null ? dto.getRegistrationAllowed() : true)
                .accessTokenLifespan(dto.getAccessTokenLifespan() != null ? dto.getAccessTokenLifespan() : 300)
                .refreshTokenLifespan(dto.getRefreshTokenLifespan() != null ? dto.getRefreshTokenLifespan() : 1800)
                .ssoSessionIdle(dto.getSsoSessionIdle() != null ? dto.getSsoSessionIdle() : 1800)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(realmService.create(realm)));
    }

    @GetMapping("/{realm}")
    public ResponseEntity<RealmDto> getRealm(@PathVariable String realm) {
        return ResponseEntity.ok(toDto(realmService.findByName(realm)));
    }

    @PutMapping("/{realm}")
    public ResponseEntity<RealmDto> updateRealm(@PathVariable String realm, @Valid @RequestBody RealmDto dto) {
        Realm existing = realmService.findByName(realm);
        Realm patch = Realm.builder()
                .displayName(dto.getDisplayName())
                .enabled(dto.getEnabled())
                .registrationAllowed(dto.getRegistrationAllowed())
                .accessTokenLifespan(dto.getAccessTokenLifespan())
                .refreshTokenLifespan(dto.getRefreshTokenLifespan())
                .ssoSessionIdle(dto.getSsoSessionIdle())
                .build();
        return ResponseEntity.ok(toDto(realmService.update(existing.getId(), patch)));
    }

    @DeleteMapping("/{realm}")
    public ResponseEntity<Void> deleteRealm(@PathVariable String realm) {
        Realm existing = realmService.findByName(realm);
        realmService.delete(existing.getId());
        return ResponseEntity.noContent().build();
    }

    private RealmDto toDto(Realm realm) {
        return RealmDto.builder()
                .id(realm.getId())
                .name(realm.getName())
                .displayName(realm.getDisplayName())
                .enabled(realm.getEnabled())
                .registrationAllowed(realm.getRegistrationAllowed())
                .accessTokenLifespan(realm.getAccessTokenLifespan())
                .refreshTokenLifespan(realm.getRefreshTokenLifespan())
                .ssoSessionIdle(realm.getSsoSessionIdle())
                .createdAt(realm.getCreatedAt())
                .updatedAt(realm.getUpdatedAt())
                .build();
    }
}
