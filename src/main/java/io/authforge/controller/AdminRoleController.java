package io.authforge.controller;

import io.authforge.domain.Role;
import io.authforge.dto.RoleDto;
import io.authforge.service.RoleService;
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
@RequestMapping("/admin/realms/{realm}/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('admin')")
public class AdminRoleController {

    private final RoleService roleService;

    @GetMapping
    public ResponseEntity<List<RoleDto>> getRoles(@PathVariable String realm) {
        List<RoleDto> roles = roleService.findByRealm(realm).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(roles);
    }

    @PostMapping
    public ResponseEntity<RoleDto> createRole(@PathVariable String realm, @Valid @RequestBody RoleDto dto) {
        Role role = Role.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .isComposite(dto.getIsComposite() != null ? dto.getIsComposite() : false)
                .clientRole(dto.getClientRole() != null ? dto.getClientRole() : false)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(roleService.create(realm, role)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoleDto> getRole(@PathVariable String realm, @PathVariable UUID id) {
        return ResponseEntity.ok(toDto(roleService.findById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoleDto> updateRole(@PathVariable String realm, @PathVariable UUID id,
                                               @RequestBody RoleDto dto) {
        Role patch = Role.builder()
                .description(dto.getDescription())
                .isComposite(dto.getIsComposite())
                .build();
        return ResponseEntity.ok(toDto(roleService.update(id, patch)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable String realm, @PathVariable UUID id) {
        roleService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private RoleDto toDto(Role role) {
        return RoleDto.builder()
                .id(role.getId())
                .realmId(role.getRealmId())
                .name(role.getName())
                .description(role.getDescription())
                .isComposite(role.getIsComposite())
                .clientRole(role.getClientRole())
                .createdAt(role.getCreatedAt())
                .build();
    }
}
