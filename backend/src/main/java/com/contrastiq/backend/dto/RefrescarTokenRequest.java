package com.contrastiq.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefrescarTokenRequest {
    @NotBlank(message = "El refresh token es obligatorio")
    private String refreshToken;
}
