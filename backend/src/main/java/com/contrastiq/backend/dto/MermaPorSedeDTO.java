package com.contrastiq.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

// Merma de insumos (contraste + solucion salina) agrupada por sede, para
// un rango de fechas dado -- permite comparar cual sede desperdicia mas.
@Getter
@AllArgsConstructor
public class MermaPorSedeDTO {
    private Long sedeId;
    private String sede;
    private BigDecimal volumenProgramadoMl;
    private BigDecimal volumenRealMl;
    private BigDecimal volumenMermaMl;
    private BigDecimal porcentajeMerma;
}
