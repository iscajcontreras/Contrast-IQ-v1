package com.contrastiq.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// "Historial de reacciones por paciente": cada evento de extravasacion
// (EDA) que tuvo esa persona, independientemente de si ya fue revisado.
@Getter
@Builder
public class ReaccionPacienteDTO {
    private Long eventoId;
    private Long inyeccionId;
    private LocalDateTime fechaHora;
    private String estadoEda;
    private Boolean revisado;
    private String accionTomada;
    private String notas;
    private String protocolo;
    private String agentePrincipal;
}
