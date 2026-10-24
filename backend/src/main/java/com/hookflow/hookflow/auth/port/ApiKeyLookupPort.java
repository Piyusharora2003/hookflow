package com.hookflow.hookflow.auth.port;

import com.hookflow.hookflow.auth.model.ApiKeyInfo;

import java.util.Optional;

/**
 * Persistence port for retrieving API key authentication information.
 */
public interface ApiKeyLookupPort {

    /**
     * Looks up an API key by its SHA-256 hash.
     *
     * <p>This method retrieves the API key metadata along with its allowed destination aliases
     * for the owning application.</p>
     *
     * @param keyHash the hex-encoded SHA-256 hash of the raw API key. Must not be null or blank.
     * @return an {@link Optional} containing {@link ApiKeyInfo} if found, or empty if no matching key exists.
     * @throws IllegalArgumentException if keyHash is null or empty.
     *
     * <b>Transaction Context:</b> Read-only operation. Does not require an active transaction, but can participate
     * in an existing one.
     * <b>Concurrency/Uniqueness:</b> key_hash is unique in storage. Safe under concurrent read access.
     */
    Optional<ApiKeyInfo> findByKeyHash(String keyHash);
}
