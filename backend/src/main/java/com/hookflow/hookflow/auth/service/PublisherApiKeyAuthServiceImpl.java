package com.hookflow.hookflow.auth.service;

import com.hookflow.hookflow.auth.cache.CaffeineApiKeyCache;
import com.hookflow.hookflow.auth.cache.RedisApiKeyCache;
import com.hookflow.hookflow.auth.model.ApiKeyInfo;
import com.hookflow.hookflow.auth.port.ApiKeyLookupPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Implementation of ApiKeyAuthService registered with bean name "publisherApiAuthService".
 */
@Service("publisherApiAuthService")
public class PublisherApiKeyAuthServiceImpl implements ApiKeyAuthService {

    private final ApiKeyLookupPort apiKeyLookupPort;
    private final CaffeineApiKeyCache l1Cache;
    private final Optional<RedisApiKeyCache> l2Cache;

    public PublisherApiKeyAuthServiceImpl(
            ApiKeyLookupPort apiKeyLookupPort,
            CaffeineApiKeyCache l1Cache,
            @Autowired(required = false) RedisApiKeyCache l2Cache
    ) {
        this.apiKeyLookupPort = apiKeyLookupPort;
        this.l1Cache = l1Cache;
        this.l2Cache = Optional.ofNullable(l2Cache);
    }

    @Override
    public Optional<ApiKeyInfo> authenticate(String rawApiKey) {
        if (rawApiKey == null || rawApiKey.isBlank()) {
            return Optional.empty();
        }

        String keyHash = hashKey(rawApiKey.trim());

        // 1. Try L1 (Caffeine)
        Optional<ApiKeyInfo> cached = l1Cache.get(keyHash);

        // 2. Try L2 (Redis) if L1 miss, populate L1 on hit
        if (cached.isEmpty() && l2Cache.isPresent()) {
            cached = l2Cache.get().get(keyHash);
            cached.ifPresent(info -> l1Cache.put(keyHash, info));
        }

        if (cached.isPresent()) {
            ApiKeyInfo info = cached.get();
            return info.isActive() ? Optional.of(info) : Optional.empty();
        }

        // 3. Query storage via port, populate both caches
        Optional<ApiKeyInfo> dbInfo = apiKeyLookupPort.findByKeyHash(keyHash);
        dbInfo.ifPresent(info -> {
            l1Cache.put(keyHash, info);
            l2Cache.ifPresent(l2 -> l2.put(keyHash, info));
        });

        return dbInfo.filter(ApiKeyInfo::isActive);
    }

    @Override
    public String hashKey(String rawApiKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(rawApiKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encodedHash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
