package com.dhs.platform.security_token_service.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${rate-limit.requests-per-minute:60}")
    private int requestsPerMinute;

    @Value("${rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    private static final String RATE_LIMIT_PREFIX = "rate_limit:";

    public boolean isRateLimited(String clientIp) {
        if (!rateLimitEnabled) {
            return false;
        }

        String key = RATE_LIMIT_PREFIX + clientIp;

        try {
            // Obter contador atual
            String currentCount = redisTemplate.opsForValue().get(key);

            if (currentCount == null) {
                // Primeira requisição, criar contador
                redisTemplate.opsForValue().set(key, "1", Duration.ofMinutes(1));
                log.debug("Rate limit iniciado para IP: {} - Contador: 1/{}", clientIp, requestsPerMinute);
                return false;
            }

            int count = Integer.parseInt(currentCount);

            if (count >= requestsPerMinute) {
                log.warn("Rate limit excedido para IP: {} - Requisições: {}/{}", clientIp, count, requestsPerMinute);
                return true;
            }

            // Incrementar contador
            redisTemplate.opsForValue().increment(key);
            log.debug("Rate limit atualizado para IP: {} - Contador: {}/{}", clientIp, count + 1, requestsPerMinute);

            return false;

        } catch (Exception e) {
            log.error("Erro ao verificar rate limit para IP: {} - {}", clientIp, e.getMessage());
            // Em caso de erro no Redis, permitir requisição
            return false;
        }
    }

    public RateLimitInfo getRateLimitInfo(String clientIp) {
        if (!rateLimitEnabled) {
            return new RateLimitInfo(requestsPerMinute, 0, -1);
        }

        String key = RATE_LIMIT_PREFIX + clientIp;

        try {
            String currentCount = redisTemplate.opsForValue().get(key);
            int count = currentCount != null ? Integer.parseInt(currentCount) : 0;
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);

            return new RateLimitInfo(requestsPerMinute, count, ttl != null ? ttl : -1);

        } catch (Exception e) {
            log.error("Erro ao obter informações de rate limit para IP: {} - {}", clientIp, e.getMessage());
            return new RateLimitInfo(requestsPerMinute, 0, -1);
        }
    }

    public void resetRateLimit(String clientIp) {
        String key = RATE_LIMIT_PREFIX + clientIp;
        redisTemplate.delete(key);
        log.info("Rate limit resetado para IP: {}", clientIp);
    }

    public static class RateLimitInfo {
        private final int limit;
        private final int current;
        private final long resetInSeconds;

        public RateLimitInfo(int limit, int current, long resetInSeconds) {
            this.limit = limit;
            this.current = current;
            this.resetInSeconds = resetInSeconds;
        }

        public int getLimit() { return limit; }
        public int getCurrent() { return current; }
        public int getRemaining() { return Math.max(0, limit - current); }
        public long getResetInSeconds() { return resetInSeconds; }
    }
}