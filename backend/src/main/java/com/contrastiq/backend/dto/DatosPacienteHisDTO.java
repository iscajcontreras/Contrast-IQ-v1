package com.contrastiq.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

// Datos que traeria el HIS del hospital para un paciente, dado su
// identificador (MRN). Ver HisIntegracionService: mientras no se
// confirme el endpoint real del HIS, esta implementacion es SIMULADA.
@Getter
@Builder
public class DatosPacienteHisDTO {
    private String identificadorExterno;
    private String nombreCompleto;
    private String sexo;
    private BigDecimal pesoKg;
    private String alergias;
    private Boolean simulado;
    private String fuente;
}
