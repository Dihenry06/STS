package com.dhs.platform.security_token_service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Slf4j
public class JwtConfigValidator {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @EventListener(ApplicationReadyEvent.class)
    public void validateJwtConfig() {
        if (!StringUtils.hasText(jwtSecret)) {
            log.error("JWT_SECRET environment variable is not set!");
            log.error("Please set JWT_SECRET environment variable with a secure secret key (minimum 256 bits / 32 characters)");
            log.error("Example: export JWT_SECRET=mySecretKey123456789012345678901234567890");
            throw new IllegalStateException("JWT_SECRET environment variable is required");
        }

        if (jwtSecret.length() < 32) {
            log.warn("JWT_SECRET is too short! Recommended minimum is 32 characters for security");
        }

        log.info("JWT configuration validated successfully");
    }
}