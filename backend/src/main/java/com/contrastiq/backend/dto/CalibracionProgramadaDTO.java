package com.contrastiq.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

// "Calendario de calibracion automatizado": proxima fecha de
// calibracion, calculada como ultima calibracion + intervalo estandar.
@Getter
@Builder
public class CalibracionProgramadaDTO {
    private Long inyectorId;
    private String numeroSerie;
    private String sala;
    private String sede;
    private LocalDate ultimaCalibracion;
    private LocalDate proximaCalibracion;
    private Long diasRestantes;
    private Boolean vencida;
}
