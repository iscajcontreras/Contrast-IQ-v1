package com.contrastiq.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class LoteDTO {
    private Long id;
    private String numeroLote;
    private String agente;
    private String sede;
    private LocalDate fechaCaducidad;
    private BigDecimal cantidadMl;
    private LocalDateTime recibidoEn;
    private Boolean activo;
    private Boolean vencido;
    private Long diasParaCaducar;
}
