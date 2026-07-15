package com.contrastiq.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OtorgarPermisoRequest {
    @NotNull
    private Long moduloId;
    @NotNull
    private Long permisoId;
}
