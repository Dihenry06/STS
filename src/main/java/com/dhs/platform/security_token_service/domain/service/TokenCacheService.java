package com.dhs.platform.security_token_service.domain.service;

import com.dhs.platform.security_token_service.adapters.in.http.dto.TokenValidationResponseDTO;
import com.dhs.platform.security_token_service.domain.port.out.cache.ITokenCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Serviço de domínio para operações de cache de tokens
 * Delega para o adapter de cache (seguindo arquitetura hexagonal)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenCacheService {

    private final ITokenCacheRepository tokenCacheRepository;

    public boolean isTokenCached(String token) {
        return tokenCacheRepository.isTokenCached(token);
    }

    public TokenValidationResponseDTO getCachedTokenValidation(String token) {
        return tokenCacheRepository.getCachedTokenValidation(token);
    }

    public void cacheTokenValidation(String token, TokenValidationResponseDTO validationResponse) {
        tokenCacheRepository.cacheTokenValidation(token, validationResponse);
    }

    public void invalidateTokenCache(String token) {
        tokenCacheRepository.invalidateTokenCache(token);
    }

    public void blacklistToken(String token, Duration duration) {
        tokenCacheRepository.blacklistToken(token, duration);
    }

    public boolean isTokenBlacklisted(String token) {
        return tokenCacheRepository.isTokenBlacklisted(token);
    }

    public Optional<String> getValidTokenForClient(String clientId) {
        return tokenCacheRepository.getValidTokenForClient(clientId);
    }

    public void cacheClientToken(String clientId, String token, Duration tokenTtl) {
        tokenCacheRepository.cacheClientToken(clientId, token, tokenTtl);
    }

    public void invalidateClientToken(String clientId) {
        tokenCacheRepository.invalidateClientToken(clientId);
    }

    public void clearAllTokenCaches() {
        tokenCacheRepository.clearAllTokenCaches();
    }
}