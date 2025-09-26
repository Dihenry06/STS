package com.dhs.platform.security_token_service.domain.port;

import com.dhs.platform.security_token_service.domain.model.Client;

import java.util.Optional;

public interface ClientRepository {
    Optional<Client> findByClientId(String clientId);
}