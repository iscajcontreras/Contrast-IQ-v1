package com.contrastiq.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Tabla detallada de merma por inyeccion individual -- para investigar
// casos puntuales (ej. procedimientos ABORTADA con merma alta). Ordenada
// por merma descendente en el repositorio (ver
// InyeccionFaseRepository.detallePorInyeccion).
@Getter
@AllArgsConstructor
public class MermaInyeccionDTO {
    private Long inyeccionId;
    private LocalDateTime fechaHoraInicio;
    private String paciente;
    private String numeroExpediente;
    private String sede;
    private String sala;
    private String estado;
    private String motivoAborto;
    private BigDecimal volumenProgramadoMl;
    private BigDecimal volumenRealMl;
    private BigDecimal volumenMermaMl;
    private BigDecimal porcentajeMerma;
}
