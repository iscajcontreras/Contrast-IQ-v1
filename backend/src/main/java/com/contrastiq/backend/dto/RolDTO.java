package com.contrastiq.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RolDTO {
    private Long id;
    private String nombre;
    // Cantidad de usuarios con este rol asignado -- se usa en la UI para
    // explicar por que un rol no se puede eliminar.
    private Long cantidadUsuarios;
}
