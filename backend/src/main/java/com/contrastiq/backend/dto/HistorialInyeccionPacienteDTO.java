package com.contrastiq.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class HistorialInyeccionPacienteDTO {
    private Long inyeccionId;
    private LocalDateTime fechaHoraInicio;
    private String sede;
    private String sala;
    private String protocolo;
    private String identificadorAnatomico;
    private String agentePrincipal;
    private BigDecimal volumenTotalMl;
    private BigDecimal dlpMgyCm;
    private BigDecimal presionMaximaPsi;
    private Boolean edaHabilitado;
    private String estado;
    private Boolean tieneAlertaEda;
    private Boolean tieneSeriePresion;
    private String operador;
}
