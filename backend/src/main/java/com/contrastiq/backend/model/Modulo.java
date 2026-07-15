package com.contrastiq.backend.model;

import jakarta.persistence.*;
import lombok.*;

// Uno de los 8 modulos reales de ContrastIQ (ver navigation.ts del
// frontend). `codigo` es el identificador estable que usa tanto el
// backend (@RequierePermiso(modulo = "...")) como Angular
// (NavigationItem.moduloCodigo / permisoGuard).
@Entity
@Table(name = "modulos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Modulo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    @Column(nullable = false)
    @Builder.Default
    private Integer orden = 0;
}
