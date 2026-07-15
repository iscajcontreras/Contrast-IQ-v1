package com.contrastiq.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TrazabilidadLoteDTO {
    private Long inyeccionId;
    private LocalDateTime fechaHoraInyeccion;
    private String pacienteIdentificador;
    private String pacienteNombre;
    private String sede;
    private String sala;
    private String inyector;
    private String protocolo;
    private String operador;
    private String estadoInyeccion;
}
