package com.contrastiq.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AlertaDTO {
    private Long id;
    private String tipo;
    private String severidad;
    private String inyector;
    private String sala;
    private String mensaje;
    private LocalDateTime fechaHora;
    private Boolean resuelta;
}
