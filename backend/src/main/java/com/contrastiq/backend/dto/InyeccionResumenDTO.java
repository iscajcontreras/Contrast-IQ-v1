package com.contrastiq.backend.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// Fila de la tabla "Inyecciones recientes" del dashboard: datos ya
// aplanados para que Angular no tenga que resolver relaciones anidadas.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InyeccionResumenDTO {
    private Long id;
    private LocalDateTime fechaHoraInicio;
    private String sala;
    private String inyector;
    private String protocolo;
    private String identificadorAnatomico;
    private String agentePrincipal;
    private BigDecimal volumenCargadoMl;
    private BigDecimal volumenTotalMl;
    private BigDecimal volumenResidualMl;
    private BigDecimal presionMaximaPsi;
    private BigDecimal presionPromedioPsi;
    private BigDecimal presionLimitePsi;
    private Boolean edaHabilitado;
    private BigDecimal ctdiVolMgy;
    private BigDecimal dlpMgyCm;
    private String estado;
    private Boolean tieneAlertaEda;
    private Boolean tieneSeriePresion;
}
