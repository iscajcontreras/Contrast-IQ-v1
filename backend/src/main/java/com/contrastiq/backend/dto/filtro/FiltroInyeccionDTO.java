package com.contrastiq.backend.dto.filtro;

import lombok.*;
import java.time.LocalDateTime;

// Representa todos los filtros que la barra de filtros del dashboard puede
// enviar. Todos los campos son opcionales (null = "todos"), tal como en el
// mockup: rango de fechas, sala/inyector, protocolo, identificador
// anatomico, agente de contraste y estado.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FiltroInyeccionDTO {
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private Long sedeId;
    private Long salaId;
    private Long inyectorId;
    private Long protocoloId;
    private Long identificadorAnatomicoId;
    private Long agenteId;
    private String estado; // COMPLETADA | ABORTADA | ERROR
    private Boolean soloConAlertaEda;
}
