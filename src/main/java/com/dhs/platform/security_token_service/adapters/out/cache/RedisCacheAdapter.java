package com.dhs.platform.security_token_service.adapters.out.cache;

import com.dhs.platform.security_token_service.domain.port.out.cache.ICacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * Adapter de saída que implementa operações de cache usando Redis
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class RedisCacheAdapter implements ICacheRepository {

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void set(String key, String value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
            log.trace("Cache set: key={}, ttl={}", key, ttl);
        } catch (Exception e) {
            log.error("Erro ao armazenar no cache: key={}, error={}", key, e.getMessage());
        }
    }

    @Override
    public Optional<String> get(String key) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            log.trace("Cache get: key={}, found={}", key, value != null);
            return Optional.ofNullable(value);
        } catch (Exception e) {
            log.error("Erro ao recuperar do cache: key={}, error={}", key, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public boolean hasKey(String key) {
        try {
            Boolean exists = redisTemplate.hasKey(key);
            return exists != null && exists;
        } catch (Exception e) {
            log.error("Erro ao verificar existência no cache: key={}, error={}", key, e.getMessage());
            return false;
        }
    }

    @Override
    public void delete(String key) {
        try {
            redisTemplate.delete(key);
            log.trace("Cache delete: key={}", key);
        } catch (Exception e) {
            log.error("Erro ao deletar do cache: key={}, error={}", key, e.getMessage());
        }
    }

    @Override
    public void delete(Set<String> keys) {
        try {
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.trace("Cache delete: keys={}", keys.size());
            }
        } catch (Exception e) {
            log.error("Erro ao deletar múltiplas chaves do cache: error={}", e.getMessage());
        }
    }

    @Override
    public Set<String> keys(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            return keys != null ? keys : Set.of();
        } catch (Exception e) {
            log.error("Erro ao buscar chaves no cache: pattern={}, error={}", pattern, e.getMessage());
            return Set.of();
        }
    }
}
