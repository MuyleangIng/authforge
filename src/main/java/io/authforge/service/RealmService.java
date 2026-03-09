package io.authforge.service;

import io.authforge.domain.Realm;
import io.authforge.exception.ResourceNotFoundException;
import io.authforge.repository.RealmRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RealmService {

    private final RealmRepository realmRepository;

    @Transactional(readOnly = true)
    public List<Realm> findAll() {
        return realmRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Realm findById(UUID id) {
        return realmRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Realm", "id", id));
    }

    @Transactional(readOnly = true)
    public Realm findByName(String name) {
        return realmRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Realm", "name", name));
    }

    @Transactional
    public Realm create(Realm realm) {
        if (realmRepository.existsByName(realm.getName())) {
            throw new IllegalArgumentException("Realm with name '" + realm.getName() + "' already exists");
        }
        Realm saved = realmRepository.save(realm);
        log.info("Created realm: {}", saved.getName());
        return saved;
    }

    @Transactional
    public Realm update(UUID id, Realm patch) {
        Realm existing = findById(id);
        if (patch.getDisplayName() != null) existing.setDisplayName(patch.getDisplayName());
        if (patch.getEnabled() != null) existing.setEnabled(patch.getEnabled());
        if (patch.getRegistrationAllowed() != null) existing.setRegistrationAllowed(patch.getRegistrationAllowed());
        if (patch.getAccessTokenLifespan() != null) existing.setAccessTokenLifespan(patch.getAccessTokenLifespan());
        if (patch.getRefreshTokenLifespan() != null) existing.setRefreshTokenLifespan(patch.getRefreshTokenLifespan());
        if (patch.getSsoSessionIdle() != null) existing.setSsoSessionIdle(patch.getSsoSessionIdle());
        return realmRepository.save(existing);
    }

    @Transactional
    public void delete(UUID id) {
        Realm realm = findById(id);
        if ("master".equals(realm.getName())) {
            throw new IllegalArgumentException("Cannot delete the master realm");
        }
        realmRepository.delete(realm);
        log.info("Deleted realm: {}", realm.getName());
    }
}
