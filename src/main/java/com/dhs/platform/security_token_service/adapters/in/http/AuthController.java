package com.dhs.platform.security_token_service.adapters.in.http;

import com.dhs.platform.security_token_service.adapters.in.http.dto.LoginRequestDTO;
import com.dhs.platform.security_token_service.adapters.in.http.dto.TokenResponseDTO;
import com.dhs.platform.security_token_service.adapters.in.http.dto.TokenValidationResponseDTO;
import com.dhs.platform.security_token_service.domain.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationService authenticationService;

    @PostMapping("/token")
    public ResponseEntity<TokenResponseDTO> authenticate(@Valid @RequestBody LoginRequestDTO request) {
        TokenResponseDTO response = authenticationService.authenticate(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate")
    public ResponseEntity<TokenValidationResponseDTO> validateToken(@RequestHeader("Authorization") String authorizationHeader) {
        TokenValidationResponseDTO response = authenticationService.validateToken(authorizationHeader);
        if (response.isValid()) {
            log.info("Token validado com sucesso para cliente: {}", response.getClientId());
            return ResponseEntity.ok(response);
        }
        log.warn("Tentativa de validação de token inválido: {}", response.getMessage());
        return ResponseEntity.status(401).body(response);
    }
}
