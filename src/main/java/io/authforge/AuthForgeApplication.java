package io.authforge;

import io.authforge.domain.Role;
import io.authforge.domain.User;
import io.authforge.repository.ClientRepository;
import io.authforge.repository.RealmRepository;
import io.authforge.repository.RoleRepository;
import io.authforge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

@SpringBootApplication
@EnableCaching
@EnableScheduling
@Slf4j
@RequiredArgsConstructor
public class AuthForgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthForgeApplication.class, args);
    }

    @Bean
    ApplicationRunner seedData(
            RealmRepository realmRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            ClientRepository clientRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            // Realm, roles, and clients are seeded by Flyway V2 and V3 migrations.
            // Here we seed the admin user if it does not already exist.
            UUID masterRealmId = UUID.fromString("00000000-0000-0000-0000-000000000001");

            if (!realmRepository.existsById(masterRealmId)) {
                log.warn("Master realm not found — Flyway migrations may not have run yet.");
                return;
            }

            if (!userRepository.existsByRealmIdAndUsername(masterRealmId, "admin")) {
                log.info("Seeding admin user for master realm...");

                Role adminRole = roleRepository.findByRealmIdAndName(masterRealmId, "admin")
                        .orElseGet(() -> roleRepository.save(Role.builder()
                                .realmId(masterRealmId)
                                .name("admin")
                                .description("Administrator role with full access")
                                .build()));

                Role userRole = roleRepository.findByRealmIdAndName(masterRealmId, "user")
                        .orElseGet(() -> roleRepository.save(Role.builder()
                                .realmId(masterRealmId)
                                .name("user")
                                .description("Default user role")
                                .build()));

                User admin = User.builder()
                        .realmId(masterRealmId)
                        .username("admin")
                        .email("admin@localhost")
                        .passwordHash(passwordEncoder.encode("admin"))
                        .firstName("Admin")
                        .lastName("User")
                        .enabled(true)
                        .emailVerified(true)
                        .build();
                admin.getRoles().add(adminRole);
                admin.getRoles().add(userRole);
                userRepository.save(admin);
                log.info("Admin user seeded successfully.");
            } else {
                log.info("Admin user already exists — skipping seed.");
            }
        };
    }
}
