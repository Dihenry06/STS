package com.dhs.platform.security_token_service.domain.port.out.cache;

import com.dhs.platform.security_token_service.adapters.in.http.dto.TokenValidationResponseDTO;

import java.time.Duration;
import java.util.Optional;

/**
 * Port de saída para cache de tokens
 */
public interface ITokenCacheRepository {

    /**
     * Verifica se um token está em cache
     */
    boolean isTokenCached(String token);

    /**
     * Recupera a validação de um token do cache
     */
    TokenValidationResponseDTO getCachedTokenValidation(String token);

    /**
     * Armazena a validação de um token no cache
     */
    void cacheTokenValidation(String token, TokenValidationResponseDTO validationResponse);

    /**
     * Invalida o cache de um token
     */
    void invalidateTokenCache(String token);

    /**
     * Adiciona um token à blacklist
     */
    void blacklistToken(String token, Duration duration);

    /**
     * Verifica se um token está na blacklist
     */
    boolean isTokenBlacklisted(String token);

    /**
     * Recupera um token válido para um cliente
     */
    Optional<String> getValidTokenForClient(String clientId);

    /**
     * Armazena a associação cliente -> token
     */
    void cacheClientToken(String clientId, String token, Duration tokenTtl);

    /**
     * Invalida o token de um cliente
     */
    void invalidateClientToken(String clientId);

    /**
     * Limpa todo o cache de tokens
     */
    void clearAllTokenCaches();
}
