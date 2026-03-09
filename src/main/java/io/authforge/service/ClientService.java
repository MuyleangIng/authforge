package io.authforge.service;

import io.authforge.domain.Client;
import io.authforge.domain.Realm;
import io.authforge.exception.ResourceNotFoundException;
import io.authforge.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientService {

    private final ClientRepository clientRepository;
    private final RealmService realmService;
    private final PasswordEncoder passwordEncoder;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Transactional(readOnly = true)
    public List<Client> findByRealm(String realmName) {
        Realm realm = realmService.findByName(realmName);
        return clientRepository.findByRealmId(realm.getId());
    }

    @Transactional(readOnly = true)
    public Client findById(UUID id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", id));
    }

    @Transactional(readOnly = true)
    public Client findByRealmAndClientId(String realmName, String clientId) {
        Realm realm = realmService.findByName(realmName);
        return clientRepository.findByRealmIdAndClientId(realm.getId(), clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "clientId", clientId));
    }

    @Transactional
    public Client create(String realmName, Client client) {
        Realm realm = realmService.findByName(realmName);
        if (clientRepository.existsByRealmIdAndClientId(realm.getId(), client.getClientId())) {
            throw new IllegalArgumentException("Client '" + client.getClientId() + "' already exists");
        }
        client.setRealmId(realm.getId());

        // Generate and hash a client secret for confidential clients
        if (!Boolean.TRUE.equals(client.getPublicClient())) {
            String rawSecret = generateSecret();
            client.setClientSecret(passwordEncoder.encode(rawSecret));
            // NOTE: the plaintext secret is returned only once — log it at DEBUG for testing
            log.debug("Generated client_secret for {} (store this): {}", client.getClientId(), rawSecret);
        }

        Client saved = clientRepository.save(client);
        log.info("Created client: {}/{}", realmName, saved.getClientId());
        return saved;
    }

    @Transactional
    public Client update(UUID id, Client patch) {
        Client existing = findById(id);
        if (patch.getName() != null) existing.setName(patch.getName());
        if (patch.getDescription() != null) existing.setDescription(patch.getDescription());
        if (patch.getEnabled() != null) existing.setEnabled(patch.getEnabled());
        if (patch.getRedirectUris() != null) existing.setRedirectUris(patch.getRedirectUris());
        if (patch.getWebOrigins() != null) existing.setWebOrigins(patch.getWebOrigins());
        if (patch.getGrantTypes() != null) existing.setGrantTypes(patch.getGrantTypes());
        return clientRepository.save(existing);
    }

    @Transactional
    public void delete(UUID id) {
        Client client = findById(id);
        clientRepository.delete(client);
        log.info("Deleted client: {}", client.getClientId());
    }

    /**
     * Regenerate the client secret. Returns the new plaintext secret (shown only once).
     */
    @Transactional
    public String regenerateSecret(UUID id) {
        Client client = findById(id);
        String rawSecret = generateSecret();
        client.setClientSecret(passwordEncoder.encode(rawSecret));
        clientRepository.save(client);
        return rawSecret;
    }

    public boolean verifyClientSecret(Client client, String rawSecret) {
        if (Boolean.TRUE.equals(client.getPublicClient())) return true;
        return passwordEncoder.matches(rawSecret, client.getClientSecret());
    }

    private String generateSecret() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
