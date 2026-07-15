package com.contrastiq.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

// "Prediccion de falla por horas de uso": estimacion simple basada en
// ciclos (inyecciones) acumulados desde el ultimo mantenimiento. No es
// un modelo de machine learning -- es un umbral de uso, consistente con
// lo que un fabricante recomienda ("cada X ciclos, dar mantenimiento").
@Getter
@Builder
public class PrediccionFallaDTO {
    private Long inyectorId;
    private String numeroSerie;
    private String sala;
    private String sede;
    private String estado;
    private Long ciclosDesdeMantenimiento;
    private Long umbralCiclos;
    private Integer porcentajeUso;
    private Boolean riesgoFalla;
    private LocalDate fechaUltimoMantenimiento;
    private Long diasDesdeMantenimiento;
}
