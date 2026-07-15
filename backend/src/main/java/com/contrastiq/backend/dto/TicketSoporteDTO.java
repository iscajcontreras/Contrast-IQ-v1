package com.contrastiq.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TicketSoporteDTO {
    private Long id;
    private Long inyectorId;
    private String inyectorNumeroSerie;
    private String sala;
    private String creadoPor;
    private String titulo;
    private String descripcion;
    private String prioridad;
    private String estado;
    private String numeroTicketFabricante;
    private String respuestaFabricante;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaCierre;
}
