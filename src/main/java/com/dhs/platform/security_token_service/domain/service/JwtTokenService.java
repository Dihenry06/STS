package com.dhs.platform.security_token_service.domain.service;

import com.dhs.platform.security_token_service.domain.model.Client;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class JwtTokenService {

    private final SecretKey secretKey;
    private final long tokenValidityInSeconds;

    public JwtTokenService(
            @Value("${jwt.secret:mySecretKey123456789012345678901234567890}") String secret,
            @Value("${jwt.expiration:3600}") long tokenValidityInSeconds
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.tokenValidityInSeconds = tokenValidityInSeconds;
    }

    public String generateToken(Client client) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiration = now.plusSeconds(tokenValidityInSeconds);

        String token = Jwts.builder()
                .subject(client.getClientId())
                .claim("client_name", client.getName())
                .claim("scopes", client.getScopes())
                .issuedAt(Date.from(now.atZone(ZoneId.systemDefault()).toInstant()))
                .expiration(Date.from(expiration.atZone(ZoneId.systemDefault()).toInstant()))
                .signWith(secretKey)
                .compact();

        log.info("Token gerado para cliente: {}", client.getClientId());
        return token;
    }

    public LocalDateTime getExpirationTime() {
        return LocalDateTime.now().plusSeconds(tokenValidityInSeconds);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token expirado: {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            log.warn("Token inválido: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Erro ao validar token: {}", e.getMessage());
            return false;
        }
    }

    public Claims extractClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            log.error("Erro ao extrair claims do token: {}", e.getMessage());
            throw new RuntimeException("Token inválido", e);
        }
    }

    public String extractClientId(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractClientName(String token) {
        return extractClaims(token).get("client_name", String.class);
    }

    @SuppressWarnings("unchecked")
    public Set<String> extractScopes(String token) {
        List<String> scopesList = extractClaims(token).get("scopes", List.class);
        return Set.copyOf(scopesList);
    }

    public LocalDateTime extractIssuedAt(String token) {
        Date issuedAt = extractClaims(token).getIssuedAt();
        return issuedAt.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    public LocalDateTime extractExpiration(String token) {
        Date expiration = extractClaims(token).getExpiration();
        return expiration.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}