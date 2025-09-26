package com.dhs.platform.security_token_service.config;

import com.dhs.platform.security_token_service.domain.model.Client;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DatabaseInitializer implements CommandLineRunner {

    private final MongoTemplate mongoTemplate;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        initializeClients();
    }

    private void initializeClients() {
        if (mongoTemplate.count(new Query(), Client.class) == 0) {
            Client client1 = Client.builder()
                    .clientId("client1")
                    .clientSecret(passwordEncoder.encode("secret1"))
                    .name("Aplicação de Exemplo 1")
                    .scopes(Set.of("read", "write"))
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            Client client2 = Client.builder()
                    .clientId("client2")
                    .clientSecret(passwordEncoder.encode("secret2"))
                    .name("Aplicação de Exemplo 2")
                    .scopes(Set.of("read"))
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            mongoTemplate.save(client1);
            mongoTemplate.save(client2);
        }
    }
}