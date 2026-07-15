package com.contrastiq.backend.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CrearChecklistRequest {

    @NotNull(message = "El paciente es obligatorio")
    private Long pacienteId;

    private Long salaId;

    @NotNull(message = "Debes confirmar la verificacion de identidad")
    @AssertTrue(message = "Debes verificar la identidad del paciente antes de continuar")
    private Boolean identidadVerificada;

    @NotNull(message = "Debes confirmar la revision del GFR")
    @AssertTrue(message = "Debes revisar el GFR del paciente antes de continuar")
    private Boolean gfrRevisado;

    @NotNull(message = "Debes confirmar la revision de alergias")
    @AssertTrue(message = "Debes revisar las alergias del paciente antes de continuar")
    private Boolean alergiasRevisadas;

    @NotNull(message = "Debes confirmar el consentimiento informado")
    @AssertTrue(message = "El consentimiento informado debe estar firmado antes de continuar")
    private Boolean consentimientoFirmado;

    private String observaciones;

    @NotBlank(message = "El nombre de quien firma es obligatorio")
    private String firmaNombre;

    @NotBlank(message = "La firma es obligatoria")
    private String firmaImagenBase64;
}
