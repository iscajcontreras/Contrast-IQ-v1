package com.contrastiq.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActualizarPedidoRequest {

    @NotBlank(message = "El estado es obligatorio")
    private String estado;
}
