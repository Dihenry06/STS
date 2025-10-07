package com.dhs.platform.security_token_service.adapters.out.cache;

import com.dhs.platform.security_token_service.domain.model.Client;
import com.dhs.platform.security_token_service.domain.port.out.cache.ICacheRepository;
import com.dhs.platform.security_token_service.domain.port.out.cache.IClientCacheRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * Adapter de saída para cache de clientes usando Redis
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class ClientCacheAdapter implements IClientCacheRepository {

    private final ICacheRepository cacheRepository;
    private final ObjectMapper objectMapper;

    @Value("${cache.client.ttl-minutes:15}")
    private int clientCacheTtlMinutes;

    @Value("${cache.client.enabled:true}")
    private boolean clientCacheEnabled;

    private static final String CLIENT_CACHE_PREFIX = "client_cache:";

    @Override
    public Optional<Client> getCachedClient(String clientId) {
        if (!clientCacheEnabled) {
            return Optional.empty();
        }

        try {
            String cacheKey = CLIENT_CACHE_PREFIX + clientId;
            Optional<String> cachedJson = cacheRepository.get(cacheKey);

            if (cachedJson.isPresent()) {
                Client client = objectMapper.readValue(cachedJson.get(), Client.class);
                log.debug("Cliente encontrado no cache: {}", clientId);
                return Optional.of(client);
            }

        } catch (Exception e) {
            log.error("Erro ao recuperar cliente do cache: {}", e.getMessage());
        }

        return Optional.empty();
    }

    @Override
    public void cacheClient(Client client) {
        if (!clientCacheEnabled || client == null) {
            return;
        }

        try {
            String cacheKey = CLIENT_CACHE_PREFIX + client.getClientId();
            String jsonValue = objectMapper.writeValueAsString(client);

            cacheRepository.set(cacheKey, jsonValue, Duration.ofMinutes(clientCacheTtlMinutes));
            log.debug("Cliente armazenado no cache por {} minutos: {}", clientCacheTtlMinutes, client.getClientId());

        } catch (Exception e) {
            log.error("Erro ao armazenar cliente no cache: {}", e.getMessage());
        }
    }

    @Override
    public void invalidateClientCache(String clientId) {
        try {
            String cacheKey = CLIENT_CACHE_PREFIX + clientId;
            cacheRepository.delete(cacheKey);
            log.debug("Cache do cliente invalidado: {}", clientId);

        } catch (Exception e) {
            log.error("Erro ao invalidar cache do cliente: {}", e.getMessage());
        }
    }

    @Override
    public void refreshClient(Client client) {
        if (client != null) {
            invalidateClientCache(client.getClientId());
            cacheClient(client);
            log.debug("Cache do cliente atualizado: {}", client.getClientId());
        }
    }

    @Override
    public void clearAllClientCaches() {
        try {
            Set<String> keys = cacheRepository.keys(CLIENT_CACHE_PREFIX + "*");
            cacheRepository.delete(keys);
            log.info("Cache de clientes limpo completamente");
        } catch (Exception e) {
            log.error("Erro ao limpar cache de clientes: {}", e.getMessage());
        }
    }

    @Override
    public long getCacheSize() {
        try {
            return cacheRepository.keys(CLIENT_CACHE_PREFIX + "*").size();
        } catch (Exception e) {
            log.error("Erro ao obter tamanho do cache: {}", e.getMessage());
            return -1;
        }
    }
}
