package com.contrastiq.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ModuloDTO {
    private Long id;
    private String codigo;
    private String nombre;
    private String descripcion;
}
