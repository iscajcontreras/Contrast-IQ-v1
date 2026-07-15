package com.contrastiq.backend.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Marca un metodo de controller como protegido por la matriz Rol x
// Modulo x Permiso (ademas de, no en lugar de, @PreAuthorize donde ya
// exista). Interceptado por PermisoAspect.
//
// Ejemplo: @RequierePermiso(modulo = "ADMINISTRACION", permiso = "EDITAR")
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RequierePermiso {
    String modulo();
    String permiso() default "VER";
}
