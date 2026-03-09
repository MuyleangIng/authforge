package io.authforge.service;

import io.authforge.domain.Realm;
import io.authforge.domain.Role;
import io.authforge.domain.User;
import io.authforge.exception.ResourceNotFoundException;
import io.authforge.repository.RoleRepository;
import io.authforge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RealmService realmService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<User> findByRealm(String realmName) {
        Realm realm = realmService.findByName(realmName);
        return userRepository.findByRealmId(realm.getId());
    }

    @Transactional(readOnly = true)
    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    @Transactional(readOnly = true)
    public User findByRealmAndUsername(String realmName, String username) {
        Realm realm = realmService.findByName(realmName);
        return userRepository.findByRealmIdAndUsername(realm.getId(), username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }

    @Transactional
    public User create(String realmName, User user, String rawPassword) {
        Realm realm = realmService.findByName(realmName);
        if (userRepository.existsByRealmIdAndUsername(realm.getId(), user.getUsername())) {
            throw new IllegalArgumentException("Username '" + user.getUsername() + "' already exists in realm '" + realmName + "'");
        }
        if (userRepository.existsByRealmIdAndEmail(realm.getId(), user.getEmail())) {
            throw new IllegalArgumentException("Email '" + user.getEmail() + "' already exists in realm '" + realmName + "'");
        }
        user.setRealmId(realm.getId());
        user.setPasswordHash(passwordEncoder.encode(rawPassword));

        // Assign default 'user' role if exists
        roleRepository.findByRealmIdAndName(realm.getId(), "user")
                .ifPresent(r -> user.getRoles().add(r));

        User saved = userRepository.save(user);
        log.info("Created user: {}/{}", realmName, saved.getUsername());
        return saved;
    }

    @Transactional
    public User update(UUID id, User patch) {
        User existing = findById(id);
        if (patch.getFirstName() != null) existing.setFirstName(patch.getFirstName());
        if (patch.getLastName() != null) existing.setLastName(patch.getLastName());
        if (patch.getEmail() != null) existing.setEmail(patch.getEmail());
        if (patch.getEnabled() != null) existing.setEnabled(patch.getEnabled());
        if (patch.getEmailVerified() != null) existing.setEmailVerified(patch.getEmailVerified());
        return userRepository.save(existing);
    }

    @Transactional
    public void delete(UUID id) {
        User user = findById(id);
        userRepository.delete(user);
        log.info("Deleted user: {}", user.getUsername());
    }

    @Transactional
    public void assignRole(UUID userId, UUID roleId) {
        User user = findById(userId);
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));
        user.getRoles().add(role);
        userRepository.save(user);
    }

    @Transactional
    public void removeRole(UUID userId, UUID roleId) {
        User user = findById(userId);
        user.getRoles().removeIf(r -> r.getId().equals(roleId));
        userRepository.save(user);
    }

    @Transactional
    public void changePassword(UUID userId, String newRawPassword) {
        User user = findById(userId);
        user.setPasswordHash(passwordEncoder.encode(newRawPassword));
        userRepository.save(user);
    }

    public boolean verifyPassword(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPasswordHash());
    }
}
