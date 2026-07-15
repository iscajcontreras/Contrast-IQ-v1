package com.contrastiq.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PermisoDTO {
    private Long id;
    private String codigo;
    private String nombre;
    private String descripcion;
}
