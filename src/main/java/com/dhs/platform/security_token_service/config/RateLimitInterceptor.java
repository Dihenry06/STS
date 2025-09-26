package com.dhs.platform.security_token_service.config;

import com.dhs.platform.security_token_service.domain.service.RateLimitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIp = getClientIp(request);

        // Verificar rate limit
        if (rateLimitService.isRateLimited(clientIp)) {
            handleRateLimitExceeded(response, clientIp);
            return false;
        }

        // Adicionar headers de rate limit
        addRateLimitHeaders(response, clientIp);
        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    private void addRateLimitHeaders(HttpServletResponse response, String clientIp) {
        RateLimitService.RateLimitInfo info = rateLimitService.getRateLimitInfo(clientIp);

        response.setHeader("X-RateLimit-Limit", String.valueOf(info.getLimit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(info.getRemaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(info.getResetInSeconds()));
    }

    private void handleRateLimitExceeded(HttpServletResponse response, String clientIp) throws IOException {
        RateLimitService.RateLimitInfo info = rateLimitService.getRateLimitInfo(clientIp);

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        // Headers de rate limit
        addRateLimitHeaders(response, clientIp);
        response.setHeader("Retry-After", String.valueOf(info.getResetInSeconds()));

        // Corpo da resposta
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Too Many Requests");
        errorResponse.put("message", "Rate limit exceeded. Try again later.");
        errorResponse.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        errorResponse.put("limit", info.getLimit());
        errorResponse.put("remaining", info.getRemaining());
        errorResponse.put("resetInSeconds", info.getResetInSeconds());

        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();

        log.warn("Rate limit exceeded for IP: {} - Limit: {}, Current: {}",
                clientIp, info.getLimit(), info.getCurrent());
    }
}