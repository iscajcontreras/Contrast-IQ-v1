package com.contrastiq.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CambiarEstadoUsuarioRequest {

    @NotNull(message = "El estado es obligatorio")
    private Boolean activo;
}
