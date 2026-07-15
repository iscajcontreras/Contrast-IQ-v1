package com.contrastiq.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

// "Reportes ejecutivos": comparativa de KPIs entre sedes para un rango
// de fechas dado.
@Getter
@Builder
public class ComparativaSedeDTO {
    private Long sedeId;
    private String sede;
    private Long totalInyecciones;
    private BigDecimal volumenTotalMl;
    private Long inyeccionesFallidas;
    private Double tasaFallaPorcentaje;
}
