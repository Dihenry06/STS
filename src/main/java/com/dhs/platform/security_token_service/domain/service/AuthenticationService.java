package com.dhs.platform.security_token_service.domain.service;

import com.dhs.platform.security_token_service.adapters.in.http.dto.LoginRequestDTO;
import com.dhs.platform.security_token_service.adapters.in.http.dto.TokenResponseDTO;
import com.dhs.platform.security_token_service.adapters.in.http.dto.TokenValidationResponseDTO;
import com.dhs.platform.security_token_service.domain.model.Client;
import com.dhs.platform.security_token_service.domain.port.in.service.IAuthenticationService;
import com.dhs.platform.security_token_service.domain.port.out.repository.IClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService implements IAuthenticationService {

    private final IClientRepository IClientRepository;
    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder;
    private final TokenCacheService tokenCacheService;
    private final ClientCacheService clientCacheService;

    public TokenResponseDTO authenticate(LoginRequestDTO request) {
        log.info("Tentativa de autenticação para cliente: {}", request.getClientId());

        // Tentar buscar cliente no cache primeiro
        Client client = clientCacheService.getCachedClient(request.getClientId())
                .orElseGet(() -> {
                    // Se não estiver em cache, buscar no banco e cachear
                    Client dbClient = IClientRepository.findByClientId(request.getClientId())
                            .orElseThrow(() -> new BadCredentialsException("Cliente não encontrado"));

                    clientCacheService.cacheClient(dbClient);
                    log.debug("Cliente carregado do banco e adicionado ao cache: {}", request.getClientId());
                    return dbClient;
                });

        if (!client.isActive()) {
            throw new BadCredentialsException("Cliente inativo");
        }

        if (!passwordEncoder.matches(request.getClientSecret(), client.getClientSecret())) {
            throw new BadCredentialsException("Credenciais inválidas");
        }

        // Verificar se cliente já tem token válido
        log.debug("Verificando token existente para cliente: {}", request.getClientId());
        Optional<String> existingToken = tokenCacheService.getValidTokenForClient(request.getClientId());

        if (existingToken.isPresent()) {
            log.info("✅ Token existente reutilizado para cliente: {}", request.getClientId());

            // Extrair dados do token existente para response
            String token = existingToken.get();
            LocalDateTime expiresAt = jwtTokenService.extractExpiration(token);

            return new TokenResponseDTO(
                    token,
                    "Bearer",
                    expiresAt
            );
        }

        // Gerar novo token se não há um válido
        String newToken = jwtTokenService.generateToken(client);
        LocalDateTime expiresAt = jwtTokenService.getExpirationTime();

        // Cachear associação cliente -> token
        Duration tokenTtl = Duration.between(LocalDateTime.now(), expiresAt);
        tokenCacheService.cacheClientToken(request.getClientId(), newToken, tokenTtl);

        log.info("🆕 Novo token gerado para cliente: {}", request.getClientId());

        return new TokenResponseDTO(
                newToken,
                "Bearer",
                expiresAt
        );
    }

    public TokenValidationResponseDTO validateToken(String token) {
        try {
            String authorization = extractTokenFromHeader(token);

            // Verificar se token está na blacklist
            if (tokenCacheService.isTokenBlacklisted(authorization)) {
                log.warn("Token na blacklist rejeitado");
                return TokenValidationResponseDTO.builder()
                        .valid(false)
                        .message("Token invalidado")
                        .build();
            }

            // Verificar se validação está em cache
            TokenValidationResponseDTO cachedValidation = tokenCacheService.getCachedTokenValidation(authorization);
            if (cachedValidation != null) {
                log.debug("Validação de token encontrada no cache");
                return cachedValidation;
            }

            // Validar token normalmente
            if (jwtTokenService.validateToken(authorization)) {
                TokenValidationResponseDTO validation = TokenValidationResponseDTO.builder()
                        .valid(true)
                        .clientId(jwtTokenService.extractClientId(authorization))
                        .clientName(jwtTokenService.extractClientName(authorization))
                        .scopes(jwtTokenService.extractScopes(authorization))
                        .issuedAt(jwtTokenService.extractIssuedAt(authorization))
                        .expiresAt(jwtTokenService.extractExpiration(authorization))
                        .message("Token válido")
                        .build();

                // Cachear validação bem-sucedida
                tokenCacheService.cacheTokenValidation(authorization, validation);
                log.debug("Validação de token armazenada no cache");

                return validation;
            }

            TokenValidationResponseDTO failedValidation = TokenValidationResponseDTO.builder()
                    .valid(false)
                    .message("Token inválido ou expirado")
                    .build();

            return failedValidation;

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