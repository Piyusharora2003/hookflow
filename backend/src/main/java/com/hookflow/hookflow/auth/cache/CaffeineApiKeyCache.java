package com.hookflow.hookflow.auth.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.hookflow.hookflow.auth.model.ApiKeyInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * In-memory L1 cache implementation backed by Caffeine.
 */
@Component
public class CaffeineApiKeyCache {

    private final Cache<String, ApiKeyInfo> cache;

    public CaffeineApiKeyCache(
            @Value("${hookflow.cache.l1.max-size:1000}") long maxSize,
            @Value("${hookflow.cache.l1.ttl-seconds:120}") long ttlSeconds,
            @Value("${hookflow.cache.negative-ttl-seconds:30}") long negativeTtlSeconds
    ) {
        long ttlNanos = Duration.ofSeconds(ttlSeconds).toNanos();
        long negativeTtlNanos = Duration.ofSeconds(negativeTtlSeconds).toNanos();

        this.cache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfter(new Expiry<String, ApiKeyInfo>() {
                    @Override
                    public long expireAfterCreate(String key, ApiKeyInfo value, long currentTime) {
                        return value.isNotFound() ? negativeTtlNanos : ttlNanos;
                    }

                    @Override
                    public long expireAfterUpdate(String key, ApiKeyInfo value, long currentTime, long currentDuration) {
                        return value.isNotFound() ? negativeTtlNanos : ttlNanos;
                    }

                    @Override
                    public long expireAfterRead(String key, ApiKeyInfo value, long currentTime, long currentDuration) {
                        return currentDuration;
                    }
                })
                .build();
    }

    public Optional<ApiKeyInfo> get(String keyHash) {
        return Optional.ofNullable(cache.getIfPresent(keyHash));
    }

    public void put(String keyHash, ApiKeyInfo apiKeyInfo) {
        if (keyHash != null && apiKeyInfo != null) {
            cache.put(keyHash, apiKeyInfo);
        }
    }

    public void evict(String keyHash) {
        if (keyHash != null) {
            cache.invalidate(keyHash);
        }
    }

    public void clear() {
        cache.invalidateAll();
    }
}
