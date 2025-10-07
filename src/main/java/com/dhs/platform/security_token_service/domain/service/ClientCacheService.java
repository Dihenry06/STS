package com.dhs.platform.security_token_service.domain.service;

import com.dhs.platform.security_token_service.domain.model.Client;
import com.dhs.platform.security_token_service.domain.port.out.cache.IClientCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Serviço de domínio para operações de cache de clientes
 * Delega para o adapter de cache (seguindo arquitetura hexagonal)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClientCacheService {

    private final IClientCacheRepository clientCacheRepository;

    public Optional<Client> getCachedClient(String clientId) {
        return clientCacheRepository.getCachedClient(clientId);
    }

    public void cacheClient(Client client) {
        clientCacheRepository.cacheClient(client);
    }

    public void invalidateClientCache(String clientId) {
        clientCacheRepository.invalidateClientCache(clientId);
    }

    public void refreshClient(Client client) {
        clientCacheRepository.refreshClient(client);
    }

    public void preloadClientsToCache() {
        // Este método pode ser chamado na inicialização para pre-carregar clientes frequentes
        log.info("Pré-carregamento de clientes no cache pode ser implementado aqui se necessário");
    }

    public void clearAllClientCaches() {
        clientCacheRepository.clearAllClientCaches();
    }

    public long getCacheSize() {
        return clientCacheRepository.getCacheSize();
    }
}