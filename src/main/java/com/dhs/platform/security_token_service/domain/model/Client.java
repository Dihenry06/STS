package com.dhs.platform.security_token_service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "clients")
public class Client {

    @Id
    private String id;

    @Indexed(unique = true)
    private String clientId;
    private String clientSecret;
    private String name;
    private Set<String> scopes;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}