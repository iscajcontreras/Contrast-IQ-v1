package com.contrastiq.backend.model;

import jakarta.persistence.*;
import lombok.*;

// Una fila = el rol tiene ese permiso en ese modulo. Otorgar = INSERT,
// revocar = DELETE (sin columna booleana "otorgado").
@Entity
@Table(name = "rol_modulo_permiso")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolModuloPermiso {

    @EmbeddedId
    private RolModuloPermisoId id;

    @MapsId("rolId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    @MapsId("moduloId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "modulo_id", nullable = false)
    private Modulo modulo;

    @MapsId("permisoId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "permiso_id", nullable = false)
    private Permiso permiso;
}
