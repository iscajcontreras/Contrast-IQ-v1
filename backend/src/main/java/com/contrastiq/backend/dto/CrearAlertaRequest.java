package com.contrastiq.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CrearAlertaRequest {

    @NotBlank
    private String tipo; // EQUIPO_MANTENIMIENTO | STOCK_BAJO | FALLA_COMUNICACION | EDA_FUERA_DE_RANGO | OTRO

    @NotBlank
    private String severidad; // INFO | ADVERTENCIA | CRITICA

    private Long inyectorId;

    @NotBlank
    @jakarta.validation.constraints.Size(max = 500)
    private String mensaje;
}
