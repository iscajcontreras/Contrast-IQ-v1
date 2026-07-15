package com.contrastiq.backend.model;

import jakarta.persistence.*;
import lombok.*;

// Catalogo global de acciones (VER, CREAR, EDITAR, ELIMINAR, EXPORTAR).
// No son por-modulo: se combinan con Modulo via RolModuloPermiso.
@Entity
@Table(name = "permisos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 255)
    private String descripcion;
}
