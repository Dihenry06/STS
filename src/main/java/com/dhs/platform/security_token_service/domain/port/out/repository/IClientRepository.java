package com.dhs.platform.security_token_service.domain.port.out.repository;

import com.dhs.platform.security_token_service.domain.model.Client;

import java.util.Optional;

public interface IClientRepository {
    Optional<Client> findByClientId(String clientId);
}