package com.contrastiq.backend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UsuarioActualDTO {
    private String nombreCompleto;
    private String email;
    private String rol;
}
