package com.dhs.platform.security_token_service.domain.port.out.cache;

import com.dhs.platform.security_token_service.domain.model.Client;

import java.util.Optional;

/**
 * Port de saída para cache de clientes
 */
public interface IClientCacheRepository {

    /**
     * Recupera um cliente do cache
     */
    Optional<Client> getCachedClient(String clientId);

    /**
     * Armazena um cliente no cache
     */
    void cacheClient(Client client);

    /**
     * Invalida o cache de um cliente
     */
    void invalidateClientCache(String clientId);

    /**
     * Atualiza o cache de um cliente
     */
    void refreshClient(Client client);

    /**
     * Limpa todo o cache de clientes
     */
    void clearAllClientCaches();

    /**
     * Retorna o tamanho do cache de clientes
     */
    long getCacheSize();
}
