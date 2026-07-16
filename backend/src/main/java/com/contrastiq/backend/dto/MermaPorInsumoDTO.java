package com.contrastiq.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

// Merma agrupada por insumo especifico (agente de contraste o solucion
// salina, por marca/fabricante) -- distingue tipo (CONTRASTE vs
// SOLUCION_SALINA, ver enum TipoAgente) para comparar cual insumo se
// desperdicia mas.
@Getter
@AllArgsConstructor
public class MermaPorInsumoDTO {
    private Long agenteId;
    private String nombreComercial;
    private String tipo;
    private String fabricante;
    private BigDecimal volumenProgramadoMl;
    private BigDecimal volumenRealMl;
    private BigDecimal volumenMermaMl;
    private BigDecimal porcentajeMerma;
}
