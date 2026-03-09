package io.authforge.repository;

import io.authforge.domain.Realm;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface RealmRepository extends JpaRepository<Realm, UUID> {
    Optional<Realm> findByName(String name);
    boolean existsByName(String name);
}
