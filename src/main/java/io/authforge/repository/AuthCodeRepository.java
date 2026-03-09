package io.authforge.repository;

import io.authforge.domain.AuthCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.Optional;

public interface AuthCodeRepository extends JpaRepository<AuthCode, String> {
    Optional<AuthCode> findByCodeAndUsedFalse(String code);

    @Modifying
    @Query("DELETE FROM AuthCode ac WHERE ac.expiresAt < :now")
    int deleteExpiredCodes(LocalDateTime now);
}
