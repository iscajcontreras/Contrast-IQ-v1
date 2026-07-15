package com.contrastiq.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

// Un punto de la grafica de presion de una inyeccion (ver manual IRiS:
// "Max. PSI... se mostrara una grafica de toda la inyeccion").
@Getter
@Builder
public class PuntoPresionDTO {
    private BigDecimal tiempoSeg;
    private BigDecimal presionPsi;
}
