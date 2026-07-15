package com.contrastiq.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Un permiso concedido en un modulo, para un rol dado. Es la forma que
// consume /api/me/permisos -- el frontend arma un Set<string> de claves
// "moduloCodigo:permisoCodigo" con esto (ver PermisosService en Angular).
@Getter
@AllArgsConstructor
public class PermisoModuloDTO {
    private String moduloCodigo;
    private String permisoCodigo;
}
