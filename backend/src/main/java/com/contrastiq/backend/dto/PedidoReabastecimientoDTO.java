package com.contrastiq.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class PedidoReabastecimientoDTO {
    private Long id;
    private String sede;
    private String agente;
    private BigDecimal cantidadSolicitadaMl;
    private String estado;
    private Boolean generadoAutomaticamente;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaEnvio;
    private LocalDateTime fechaRecepcion;
    private String notas;
}
