package com.contrastiq.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActualizarUsuarioRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombreCompleto;

    @NotNull(message = "El rol es obligatorio")
    private Long rolId;

    private Long sedeId;
}
