package com.dhs.platform.security_token_service.domain.service;

import com.dhs.platform.security_token_service.adapters.in.http.dto.LoginRequestDTO;
import com.dhs.platform.security_token_service.adapters.in.http.dto.TokenResponseDTO;
import com.dhs.platform.security_token_service.adapters.in.http.dto.TokenValidationResponseDTO;
import com.dhs.platform.security_token_service.domain.model.Client;
import com.dhs.platform.security_token_service.domain.port.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final ClientRepository clientRepository;
    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder;

    public TokenResponseDTO authenticate(LoginRequestDTO request) {
        log.info("Tentativa de autenticação para cliente: {}", request.getClientId());

        Client client = clientRepository.findByClientId(request.getClientId())
                .orElseThrow(() -> new BadCredentialsException("Cliente não encontrado"));

        if (!client.isActive()) {
            throw new BadCredentialsException("Cliente inativo");
        }

        if (!passwordEncoder.matches(request.getClientSecret(), client.getClientSecret())) {
            throw new BadCredentialsException("Credenciais inválidas");
        }

        String token = jwtTokenService.generateToken(client);

        log.info("Autenticação bem-sucedida para cliente: {}", request.getClientId());

        return new TokenResponseDTO(
                token,
                "Bearer",
                jwtTokenService.getExpirationTime()
        );
    }

    public TokenValidationResponseDTO validateToken(String token) {
        try {
            String authorization = extractTokenFromHeader(token);
            if (jwtTokenService.validateToken(authorization)) {
                return TokenValidationResponseDTO.builder()
                        .valid(true)
                        .clientId(jwtTokenService.extractClientId(authorization))
                        .clientName(jwtTokenService.extractClientName(authorization))
                        .scopes(jwtTokenService.extractScopes(authorization))
                        .issuedAt(jwtTokenService.extractIssuedAt(authorization))
                        .expiresAt(jwtTokenService.extractExpiration(authorization))
                        .message("Token válido")
                        .build();
            }
            return TokenValidationResponseDTO.builder()
                    .valid(false)
                    .message("Token inválido ou expirado")
                    .build();
        } catch (Exception e) {
            log.error("Erro ao validar token: {}", e.getMessage());
            return TokenValidationResponseDTO.builder()
                    .valid(false)
                    .message("Erro ao processar token: " + e.getMessage())
                    .build();
        }
    }

    private String extractTokenFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Header Authorization deve conter um Bearer token");
        }
        return authorizationHeader.substring(7);
    }
}