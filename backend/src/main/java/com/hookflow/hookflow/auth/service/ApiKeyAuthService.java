package com.hookflow.hookflow.auth.service;

import com.hookflow.hookflow.auth.model.ApiKeyInfo;

import java.util.Optional;

/**
 * Service interface for authenticating incoming API keys.
 */
public interface ApiKeyAuthService {

    /**
     * Authenticates a raw API key.
     * Computes SHA-256 hash, looks up from cache (L1/L2) or database,
     * and validates active status and expiration.
     *
     * @param rawApiKey the raw token string from the Authorization Bearer header
     * @return an {@link Optional} containing {@link ApiKeyInfo} if valid and active, empty otherwise
     */
    Optional<ApiKeyInfo> authenticate(String rawApiKey);

    /**
     * Computes the hex-encoded SHA-256 hash of a raw API key.
     *
     * @param rawApiKey raw API key string
     * @return lowercase hex-encoded SHA-256 hash
     */
    String hashKey(String rawApiKey);
}
