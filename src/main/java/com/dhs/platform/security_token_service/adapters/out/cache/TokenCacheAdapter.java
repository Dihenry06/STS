package com.dhs.platform.security_token_service.adapters.out.cache;

import com.dhs.platform.security_token_service.adapters.in.http.dto.TokenValidationResponseDTO;
import com.dhs.platform.security_token_service.domain.port.out.cache.ICacheRepository;
import com.dhs.platform.security_token_service.domain.port.out.cache.ITokenCacheRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;

/**
 * Adapter de saída para cache de tokens usando Redis
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class TokenCacheAdapter implements ITokenCacheRepository {

    private static final String TOKEN_CACHE_PREFIX = "token_cache:";
    private static final String TOKEN_BLACKLIST_PREFIX = "token_blacklist:";
    private static final String CLIENT_TOKEN_PREFIX = "client_token:";

    private final ICacheRepository cacheRepository;
    private final ObjectMapper objectMapper;

    @Value("${cache.token.ttl-minutes:5}")
    private int tokenCacheTtlMinutes;

    @Value("${cache.token.enabled:true}")
    private boolean tokenCacheEnabled;

    @Override
    public boolean isTokenCached(String token) {
        if (!tokenCacheEnabled) {
            return false;
        }

        try {
            String tokenHash = hashToken(token);
            String cacheKey = TOKEN_CACHE_PREFIX + tokenHash;
            return cacheRepository.hasKey(cacheKey);
        } catch (Exception e) {
            log.error("Erro ao verificar cache do token: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public TokenValidationResponseDTO getCachedTokenValidation(String token) {
        if (!tokenCacheEnabled) {
            return null;
        }

        try {
            String tokenHash = hashToken(token);
            String cacheKey = TOKEN_CACHE_PREFIX + tokenHash;

            Optional<String> cachedJson = cacheRepository.get(cacheKey);
            if (cachedJson.isPresent()) {
                TokenValidationResponseDTO cached = objectMapper.readValue(cachedJson.get(), TokenValidationResponseDTO.class);

                // Verificar se não expirou baseado na data de expiração do token
                if (cached.getExpiresAt() != null && cached.getExpiresAt().isAfter(LocalDateTime.now())) {
                    log.debug("Token encontrado no cache: {}", tokenHash.substring(0, 8) + "...");
                    return cached;
                } else {
                    // Token expirou, remover do cache
                    invalidateTokenCache(token);
                    log.debug("Token expirado removido do cache: {}", tokenHash.substring(0, 8) + "...");
                }
            }
        } catch (Exception e) {
            log.error("Erro ao recuperar token do cache: {}", e.getMessage());
        }

        return null;
    }

    @Override
    public void cacheTokenValidation(String token, TokenValidationResponseDTO validationResponse) {
        if (!tokenCacheEnabled || !validationResponse.isValid()) {
            return;
        }

        try {
            String tokenHash = hashToken(token);
            String cacheKey = TOKEN_CACHE_PREFIX + tokenHash;

            String jsonValue = objectMapper.writeValueAsString(validationResponse);

            // TTL baseado no menor valor entre: TTL configurado ou tempo até expiração do token
            Duration ttl = calculateTtl(validationResponse.getExpiresAt());

            cacheRepository.set(cacheKey, jsonValue, ttl);
            log.debug("Token armazenado no cache por {} minutos: {}", ttl.toMinutes(), tokenHash.substring(0, 8) + "...");

        } catch (Exception e) {
            log.error("Erro ao armazenar token no cache: {}", e.getMessage());
        }
    }

    @Override
    public void invalidateTokenCache(String token) {
        try {
            String tokenHash = hashToken(token);
            String cacheKey = TOKEN_CACHE_PREFIX + tokenHash;

            cacheRepository.delete(cacheKey);
            log.debug("Token removido do cache: {}", tokenHash.substring(0, 8) + "...");

        } catch (Exception e) {
            log.error("Erro ao invalidar token do cache: {}", e.getMessage());
        }
    }

    @Override
    public void blacklistToken(String token, Duration duration) {
        try {
            String tokenHash = hashToken(token);
            String blacklistKey = TOKEN_BLACKLIST_PREFIX + tokenHash;

            cacheRepository.set(blacklistKey, "blacklisted", duration);

            // Também remover do cache de validação
            invalidateTokenCache(token);

            log.info("Token adicionado à blacklist por {} minutos: {}", duration.toMinutes(), tokenHash.substring(0, 8) + "...");

        } catch (Exception e) {
            log.error("Erro ao adicionar token à blacklist: {}", e.getMessage());
        }
    }

    @Override
    public boolean isTokenBlacklisted(String token) {
        try {
            String tokenHash = hashToken(token);
            String blacklistKey = TOKEN_BLACKLIST_PREFIX + tokenHash;

            return cacheRepository.hasKey(blacklistKey);

        } catch (Exception e) {
            log.error("Erro ao verificar blacklist do token: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public Optional<String> getValidTokenForClient(String clientId) {
        if (!tokenCacheEnabled) {
            log.debug("❌ Cache de tokens desabilitado");
            return Optional.empty();
        }

        try {
            String clientTokenKey = CLIENT_TOKEN_PREFIX + clientId;
            log.debug("🔍 Procurando token para cliente: {} (chave: {})", clientId, clientTokenKey);

            Optional<String> existingToken = cacheRepository.get(clientTokenKey);

            if (existingToken.isPresent()) {
                String token = existingToken.get();
                log.debug("📦 Token encontrado no cache para cliente: {}", clientId);

                // Verificar se o token ainda é válido (não está na blacklist e não expirou)
                if (!isTokenBlacklisted(token)) {
                    log.debug("✅ Token não está na blacklist");

                    // Verificar se ainda está no cache de validação (significa que é válido)
                    TokenValidationResponseDTO cached = getCachedTokenValidation(token);
                    if (cached != null && cached.isValid()) {
                        log.debug("✅ Token válido encontrado no cache de validação para cliente: {}", clientId);
                        return Optional.of(token);
                    } else {
                        log.debug("❌ Token não encontrado no cache de validação ou inválido");
                    }
                } else {
                    log.debug("❌ Token está na blacklist");
                }

                // Token inválido ou expirado, remover referência
                log.debug("🗑️ Removendo token inválido para cliente: {}", clientId);
                invalidateClientToken(clientId);
            } else {
                log.debug("❌ Nenhum token encontrado no cache para cliente: {}", clientId);
            }

        } catch (Exception e) {
            log.error("💥 Erro ao verificar token existente para cliente: {} - {}", clientId, e.getMessage());
        }

        return Optional.empty();
    }

    @Override
    public void cacheClientToken(String clientId, String token, Duration tokenTtl) {
        if (!tokenCacheEnabled) {
            log.debug("❌ Cache de tokens desabilitado - não armazenando token para cliente: {}", clientId);
            return;
        }

        try {
            String clientTokenKey = CLIENT_TOKEN_PREFIX + clientId;

            // Armazenar referência do cliente -> token
            cacheRepository.set(clientTokenKey, token, tokenTtl);

            log.debug("💾 Token associado ao cliente: {} por {} minutos (chave: {})", clientId, tokenTtl.toMinutes(), clientTokenKey);

        } catch (Exception e) {
            log.error("💥 Erro ao associar token ao cliente: {} - {}", clientId, e.getMessage());
        }
    }

    @Override
    public void invalidateClientToken(String clientId) {
        try {
            String clientTokenKey = CLIENT_TOKEN_PREFIX + clientId;
            Optional<String> existingToken = cacheRepository.get(clientTokenKey);

            if (existingToken.isPresent()) {
                // Invalidar cache do token
                invalidateTokenCache(existingToken.get());
                // Remover associação cliente -> token
                cacheRepository.delete(clientTokenKey);
                log.debug("Token do cliente invalidado: {}", clientId);
            }

        } catch (Exception e) {
            log.error("Erro ao invalidar token do cliente: {} - {}", clientId, e.getMessage());
        }
    }

    @Override
    public void clearAllTokenCaches() {
        try {
            Set<String> tokenCacheKeys = cacheRepository.keys(TOKEN_CACHE_PREFIX + "*");
            Set<String> blacklistKeys = cacheRepository.keys(TOKEN_BLACKLIST_PREFIX + "*");
            Set<String> clientTokenKeys = cacheRepository.keys(CLIENT_TOKEN_PREFIX + "*");

            cacheRepository.delete(tokenCacheKeys);
            cacheRepository.delete(blacklistKeys);
            cacheRepository.delete(clientTokenKeys);

            log.info("Cache de tokens limpo completamente");
        } catch (Exception e) {
            log.error("Erro ao limpar cache de tokens: {}", e.getMessage());
        }
    }

    private String hashToken(String token) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(token.getBytes());
        return Base64.getEncoder().encodeToString(hash);
    }

    private Duration calculateTtl(LocalDateTime tokenExpiration) {
        if (tokenExpiration == null) {
            return Duration.ofMinutes(tokenCacheTtlMinutes);
        }

        Duration timeUntilExpiration = Duration.between(LocalDateTime.now(), tokenExpiration);
        Duration configuredTtl = Duration.ofMinutes(tokenCacheTtlMinutes);

        // Usar o menor entre o TTL configurado e o tempo até expiração do token
        return timeUntilExpiration.compareTo(configuredTtl) < 0 ? timeUntilExpiration : configuredTtl;
    }
}
