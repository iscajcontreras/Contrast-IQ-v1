package com.contrastiq.backend.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RevisarExtravasacionRequest {

    @Size(max = 255)
    private String accionTomada;

    @Size(max = 2000)
    private String notas;
}
