package io.authforge.repository;

import io.authforge.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByRealmIdAndUsername(UUID realmId, String username);
    Optional<User> findByRealmIdAndEmail(UUID realmId, String email);
    List<User> findByRealmId(UUID realmId);
    boolean existsByRealmIdAndUsername(UUID realmId, String username);
    boolean existsByRealmIdAndEmail(UUID realmId, String email);
    long countByRealmId(UUID realmId);
}
