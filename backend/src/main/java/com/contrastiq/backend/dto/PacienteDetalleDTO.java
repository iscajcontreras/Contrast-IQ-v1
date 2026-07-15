package com.contrastiq.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class PacienteDetalleDTO {
    private Long id;
    private String identificadorExterno;
    private String nombreCompleto;
    private String sexo;
    private BigDecimal pesoKg;
    private BigDecimal creatininaMgDl;
    private BigDecimal gfrMlMin;
    private Boolean riesgoRenal; // GFR bajo -> requiere precaucion con el contraste

    // Indicadores calculados a partir de su historial
    private Long totalInyecciones;
    private BigDecimal volumenTotalRecibidoMl;
    private BigDecimal dlpTotalMgyCm;
    private LocalDateTime ultimaInyeccion;
    private Long alertasEdaFueraDeRango;
    private Long inyeccionesAbortadasOError;
}
