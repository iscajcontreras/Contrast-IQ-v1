package com.contrastiq.backend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PacienteResumenDTO {
    private Long id;
    private String identificadorExterno;
    private String nombreCompleto;
    private String sexo;
}
