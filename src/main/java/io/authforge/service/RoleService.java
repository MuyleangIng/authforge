package io.authforge.service;

import io.authforge.domain.Realm;
import io.authforge.domain.Role;
import io.authforge.exception.ResourceNotFoundException;
import io.authforge.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleService {

    private final RoleRepository roleRepository;
    private final RealmService realmService;

    @Transactional(readOnly = true)
    public List<Role> findByRealm(String realmName) {
        Realm realm = realmService.findByName(realmName);
        return roleRepository.findByRealmId(realm.getId());
    }

    @Transactional(readOnly = true)
    public Role findById(UUID id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", id));
    }

    @Transactional(readOnly = true)
    public Role findByRealmAndName(String realmName, String roleName) {
        Realm realm = realmService.findByName(realmName);
        return roleRepository.findByRealmIdAndName(realm.getId(), roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", roleName));
    }

    @Transactional
    public Role create(String realmName, Role role) {
        Realm realm = realmService.findByName(realmName);
        if (roleRepository.existsByRealmIdAndName(realm.getId(), role.getName())) {
            throw new IllegalArgumentException("Role '" + role.getName() + "' already exists in realm '" + realmName + "'");
        }
        role.setRealmId(realm.getId());
        Role saved = roleRepository.save(role);
        log.info("Created role: {}/{}", realmName, saved.getName());
        return saved;
    }

    @Transactional
    public Role update(UUID id, Role patch) {
        Role existing = findById(id);
        if (patch.getDescription() != null) existing.setDescription(patch.getDescription());
        if (patch.getIsComposite() != null) existing.setIsComposite(patch.getIsComposite());
        return roleRepository.save(existing);
    }

    @Transactional
    public void delete(UUID id) {
        Role role = findById(id);
        roleRepository.delete(role);
        log.info("Deleted role: {}", role.getName());
    }
}
