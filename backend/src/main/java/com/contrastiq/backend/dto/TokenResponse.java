package com.contrastiq.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    // Segundos de vida del access token, para que el front sepa cuando renovarlo solo.
    private long expiresIn;
}
