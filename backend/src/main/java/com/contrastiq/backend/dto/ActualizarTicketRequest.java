package com.contrastiq.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActualizarTicketRequest {

    @NotBlank(message = "El estado es obligatorio")
    private String estado;

    @Size(max = 100)
    private String numeroTicketFabricante;

    @Size(max = 2000)
    private String respuestaFabricante;
}
