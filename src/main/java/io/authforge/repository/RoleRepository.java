package io.authforge.repository;

import io.authforge.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {
    Optional<Role> findByRealmIdAndName(UUID realmId, String name);
    List<Role> findByRealmId(UUID realmId);
    boolean existsByRealmIdAndName(UUID realmId, String name);
}
