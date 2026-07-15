package com.contrastiq.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    @NotBlank(message = "El correo es obligatorio")
    private String email;

    @NotBlank(message = "La contrasena es obligatoria")
    private String password;
}
