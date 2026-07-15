package com.contrastiq.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Una celda de la matriz Rol x Modulo x Permiso para la pantalla
// "Roles y permisos": indica si el rol consultado tiene o no ese permiso
// en ese modulo, para pintar el checkbox correspondiente.
@Getter
@AllArgsConstructor
public class MatrizCeldaDTO {
    private Long moduloId;
    private String moduloCodigo;
    private String moduloNombre;
    private Long permisoId;
    private String permisoCodigo;
    private String permisoNombre;
    private boolean otorgado;
}
