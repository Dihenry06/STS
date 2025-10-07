package com.dhs.platform.security_token_service.domain.port.out.cache;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * Port de saída para operações de cache
 * Abstração que permite diferentes implementações (Redis, Memcached, etc.)
 */
public interface ICacheRepository {

    /**
     * Armazena um valor no cache com TTL
     */
    void set(String key, String value, Duration ttl);

    /**
     * Recupera um valor do cache
     */
    Optional<String> get(String key);

    /**
     * Verifica se uma chave existe no cache
     */
    boolean hasKey(String key);

    /**
     * Remove uma chave do cache
     */
    void delete(String key);

    /**
     * Remove múltiplas chaves do cache
     */
    void delete(Set<String> keys);

    /**
     * Busca chaves por padrão
     */
    Set<String> keys(String pattern);
}
