package com.contrastiq.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UsuarioResumenDTO {
    private Long id;
    private String nombreCompleto;
    private String email;
    private String rol;
    private String sede;
    private Long sedeId;
    private Boolean activo;
    private String proveedor;
    // "En linea" = tiene al menos un refresh token vigente (ver
    // TokenRefrescoRepository.existsByUsuario_Id...). No es presencia en
    // tiempo real, es "tiene sesion valida ahora mismo".
    private Boolean online;
    private LocalDateTime ultimoLogin;
}
