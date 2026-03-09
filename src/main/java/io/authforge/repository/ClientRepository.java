package io.authforge.repository;

import io.authforge.domain.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientRepository extends JpaRepository<Client, UUID> {
    Optional<Client> findByRealmIdAndClientId(UUID realmId, String clientId);
    List<Client> findByRealmId(UUID realmId);
    boolean existsByRealmIdAndClientId(UUID realmId, String clientId);
}
