package com.contrastiq.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class ChecklistPreInyeccionDTO {
    private Long id;
    private Long pacienteId;
    private String sala;
    private String operador;
    private Boolean identidadVerificada;
    private Boolean gfrRevisado;
    private BigDecimal gfrValorMomento;
    private Boolean riesgoRenalMomento;
    private Boolean alergiasRevisadas;
    private String alergiasMomento;
    private Boolean consentimientoFirmado;
    private String observaciones;
    private String firmaNombre;
    private String firmaImagenBase64;
    private LocalDateTime fechaHora;
}
