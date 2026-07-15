package com.contrastiq.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CrearTicketRequest {

    @NotNull(message = "El inyector es obligatorio")
    private Long inyectorId;

    @NotBlank(message = "El titulo es obligatorio")
    @Size(max = 200)
    private String titulo;

    @NotBlank(message = "La descripcion es obligatoria")
    @Size(max = 2000)
    private String descripcion;

    @NotBlank(message = "La prioridad es obligatoria")
    private String prioridad;
}
