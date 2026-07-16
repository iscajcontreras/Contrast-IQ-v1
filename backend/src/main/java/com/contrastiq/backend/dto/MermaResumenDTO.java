package com.contrastiq.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

// KPI agregado de merma de insumos (contraste + solucion salina) para un
// rango de fechas, con tendencia contra el periodo inmediato anterior de
// la misma duracion (ej. si "desde/hasta" son 30 dias, se compara contra
// los 30 dias previos a "desde").
@Getter
@AllArgsConstructor
public class MermaResumenDTO {
    private BigDecimal volumenProgramadoMl;
    private BigDecimal volumenRealMl;
    private BigDecimal volumenMermaMl;
    private BigDecimal porcentajeMerma;
    // Merma del periodo anterior equivalente, para la tendencia. Null si
    // no aplica (ej. rango personalizado sin punto de comparacion claro).
    private BigDecimal volumenMermaPeriodoAnteriorMl;
    // Positivo = la merma aumento vs el periodo anterior (peor), negativo = mejoro.
    private BigDecimal variacionPorcentual;
}
