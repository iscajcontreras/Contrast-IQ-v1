package com.contrastiq.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class HistorialAccesoDTO {
    private Long id;
    private String emailUsado;
    private Boolean exitoso;
    private String metodo;
    private String ipOrigen;
    private String userAgent;
    private LocalDateTime fechaHora;
}
