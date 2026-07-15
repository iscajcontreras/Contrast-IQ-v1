package com.contrastiq.backend.dto;

import lombok.*;
import java.math.BigDecimal;

// Alimenta las 4 tarjetas KPI de la parte superior del dashboard.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardKpiDTO {
    private Long inyeccionesEnPeriodo;
    private BigDecimal volumenTotalMl;
    private BigDecimal volumenPromedioMl;
    private Long alertasEdaFueraDeRango;
    private Long inyectoresActivos;
    private Long inyectoresTotales;
}
