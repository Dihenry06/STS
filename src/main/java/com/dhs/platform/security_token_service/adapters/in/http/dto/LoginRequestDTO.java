package com.dhs.platform.security_token_service.adapters.in.http.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDTO {

    @NotBlank(message = "Client ID é obrigatório")
    private String clientId;

    @NotBlank(message = "Client Secret é obrigatório")
    private String clientSecret;
}
