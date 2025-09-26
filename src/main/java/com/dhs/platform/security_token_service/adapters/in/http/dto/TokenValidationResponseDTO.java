package com.dhs.platform.security_token_service.adapters.in.http.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenValidationResponseDTO {

    private boolean valid;
    private String clientId;
    private String clientName;
    private Set<String> scopes;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
    private String message;
}