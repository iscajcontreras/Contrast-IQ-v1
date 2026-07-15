package com.contrastiq.backend.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CrearLoteRequest {

    @NotNull(message = "El agente de contraste es obligatorio")
    private Long agenteId;

    @NotNull(message = "La sede es obligatoria")
    private Long sedeId;

    @NotBlank(message = "El numero de lote es obligatorio")
    private String numeroLote;

    @NotNull(message = "La fecha de caducidad es obligatoria")
    @Future(message = "La fecha de caducidad debe ser futura")
    private LocalDate fechaCaducidad;

    @NotNull(message = "La cantidad es obligatoria")
    @Positive(message = "La cantidad debe ser mayor a 0")
    private BigDecimal cantidadMl;
}
