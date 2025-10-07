package com.dhs.platform.security_token_service.domain.port.in.service;

import com.dhs.platform.security_token_service.adapters.in.http.dto.LoginRequestDTO;
import com.dhs.platform.security_token_service.adapters.in.http.dto.TokenResponseDTO;
import com.dhs.platform.security_token_service.adapters.in.http.dto.TokenValidationResponseDTO;

public interface IAuthenticationService {
    TokenResponseDTO authenticate(LoginRequestDTO request);

    TokenValidationResponseDTO validateToken(String token);
}
