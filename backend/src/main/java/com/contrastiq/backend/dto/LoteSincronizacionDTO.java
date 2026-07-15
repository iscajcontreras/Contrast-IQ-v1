package com.contrastiq.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class LoteSincronizacionDTO {
    private Long id;
    private String fuente;
    private LocalDateTime fechaHora;
    private Integer registrosImportados;
    private String estado;
    private String usuario;
    private String detalle;
}
