package com.contrastiq.backend.model;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

// Llave compuesta de rol_modulo_permiso. equals/hashCode manuales (no
// generados por Lombok con @EqualsAndHashCode a proposito, para evitar
// el problema clasico de @Embeddable + proxies de Hibernate rompiendo la
// igualdad por referencia de clase) -- patron tomado del proyecto de
// referencia CEROGAS GPS.
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RolModuloPermisoId implements Serializable {

    private Long rolId;
    private Long moduloId;
    private Long permisoId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RolModuloPermisoId that = (RolModuloPermisoId) o;
        return Objects.equals(rolId, that.rolId)
                && Objects.equals(moduloId, that.moduloId)
                && Objects.equals(permisoId, that.permisoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rolId, moduloId, permisoId);
    }
}
