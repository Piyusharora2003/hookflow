package com.hookflow.hookflow.auth.cache;

import com.hookflow.hookflow.auth.model.ApiKeyInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;

/**
 * Distributed L2 cache implementation backed by Redis.
 * Only loaded when hookflow.cache.l2.enabled=true.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "hookflow.cache.l2", name = "enabled", havingValue = "true")
public class RedisApiKeyCache {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final long ttlSeconds;
    private final String keyPrefix;

    public RedisApiKeyCache(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${hookflow.cache.l2.ttl-seconds:300}") long ttlSeconds,
            @Value("${hookflow.cache.l2.key-prefix:hookflow:apikey:}") String keyPrefix
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ttlSeconds = ttlSeconds;
        this.keyPrefix = keyPrefix;
    }

    public Optional<ApiKeyInfo> get(String keyHash) {
        try {
            String redisKey = keyPrefix + keyHash;
            String json = redisTemplate.opsForValue().get(redisKey);
            if (json == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(objectMapper.readValue(json, ApiKeyInfo.class));
        } catch (Exception e) {
            log.warn("Failed to read API key from Redis L2 cache: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public void put(String keyHash, ApiKeyInfo apiKeyInfo) {
        try {
            String redisKey = keyPrefix + keyHash;
            String json = objectMapper.writeValueAsString(apiKeyInfo);
            redisTemplate.opsForValue().set(
                    redisKey,
                    json,
                    Duration.ofSeconds(ttlSeconds)
            );
        } catch (Exception e) {
            log.warn("Failed to write API key to Redis L2 cache: {}", e.getMessage());
        }
    }

    public void evict(String keyHash) {
        try {
            String redisKey = keyPrefix + keyHash;
            redisTemplate.delete(redisKey);
        } catch (Exception e) {
            log.warn("Failed to evict API key from Redis L2 cache: {}", e.getMessage());
        }
    }
}
