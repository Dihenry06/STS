package com.dhs.platform.security_token_service.adapters.in.http;

import com.dhs.platform.security_token_service.domain.service.ClientCacheService;
import com.dhs.platform.security_token_service.domain.service.RateLimitService;
import com.dhs.platform.security_token_service.domain.service.TokenCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "admin.endpoints.enabled", havingValue = "true")
public class AdminController {

    private final TokenCacheService tokenCacheService;
    private final ClientCacheService clientCacheService;
    private final RateLimitService rateLimitService;

    @PostMapping("/cache/tokens/clear")
    public ResponseEntity<Map<String, String>> clearTokenCache() {
        tokenCacheService.clearAllTokenCaches();
        log.info("Cache de tokens limpo via admin endpoint");

        Map<String, String> response = new HashMap<>();
        response.put("message", "Cache de tokens limpo com sucesso");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/cache/clients/clear")
    public ResponseEntity<Map<String, String>> clearClientCache() {
        clientCacheService.clearAllClientCaches();
        log.info("Cache de clientes limpo via admin endpoint");

        Map<String, String> response = new HashMap<>();
        response.put("message", "Cache de clientes limpo com sucesso");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/rate-limit/{ip}/reset")
    public ResponseEntity<Map<String, String>> resetRateLimit(@PathVariable String ip) {
        rateLimitService.resetRateLimit(ip);
        log.info("Rate limit resetado para IP: {} via admin endpoint", ip);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Rate limit resetado para IP: " + ip);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/tokens/{token}/blacklist")
    public ResponseEntity<Map<String, String>> blacklistToken(
            @PathVariable String token,
            @RequestParam(defaultValue = "3600") int durationMinutes) {

        tokenCacheService.blacklistToken("Bearer " + token, Duration.ofMinutes(durationMinutes));
        log.warn("Token adicionado à blacklist via admin endpoint por {} minutos", durationMinutes);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Token adicionado à blacklist por " + durationMinutes + " minutos");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/cache/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("clientCacheSize", clientCacheService.getCacheSize());
        stats.put("message", "Estatísticas de cache obtidas com sucesso");

        log.debug("Estatísticas de cache solicitadas via admin endpoint");
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/cache/invalidate/client/{clientId}")
    public ResponseEntity<Map<String, String>> invalidateClientCache(@PathVariable String clientId) {
        clientCacheService.invalidateClientCache(clientId);
        tokenCacheService.invalidateClientToken(clientId);
        log.info("Cache e token invalidados para cliente: {} via admin endpoint", clientId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Cache e token invalidados para cliente: " + clientId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/client/{clientId}/revoke-token")
    public ResponseEntity<Map<String, String>> revokeClientToken(@PathVariable String clientId) {
        tokenCacheService.invalidateClientToken(clientId);
        log.info("Token revogado para cliente: {} via admin endpoint", clientId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Token revogado para cliente: " + clientId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/debug/cache/{clientId}")
    public ResponseEntity<Map<String, Object>> debugTokenCache(@PathVariable String clientId) {
        Map<String, Object> debug = new HashMap<>();

        try {
            // Verificar se tem token válido
            boolean hasValidToken = tokenCacheService.getValidTokenForClient(clientId).isPresent();
            debug.put("hasValidToken", hasValidToken);

            // Informações gerais
            debug.put("clientId", clientId);
            debug.put("timestamp", java.time.LocalDateTime.now());

            log.info("Debug cache solicitado para cliente: {}", clientId);

        } catch (Exception e) {
            debug.put("error", e.getMessage());
            log.error("Erro no debug do cache para cliente: {}", clientId, e);
        }

        return ResponseEntity.ok(debug);
    }
}