package com.dhs.platform.security_token_service.adapters.out.repository;

import com.dhs.platform.security_token_service.domain.model.Client;
import com.dhs.platform.security_token_service.domain.port.out.repository.IClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Primary
@RequiredArgsConstructor
public class ClientRepository implements IClientRepository {

    private final MongoTemplate mongoTemplate;

    @Override
    public Optional<Client> findByClientId(String clientId) {
        Query query = new Query(Criteria.where("clientId").is(clientId));
        Client client = mongoTemplate.findOne(query, Client.class);
        return Optional.ofNullable(client);
    }
}