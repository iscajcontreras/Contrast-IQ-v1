package com.contrastiq.backend.model;

import com.contrastiq.backend.model.enums.ProveedorAutenticacion;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_id")
    private Sede sede;

    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    // Unico valor posible hoy es LOCAL (ver ProveedorAutenticacion) -- la
    // columna en BD sigue siendo ENUM('LOCAL','GOOGLE') por compatibilidad,
    // pero el login federado con Google nunca se implemento y se removio
    // como limpieza de deuda tecnica (Prioridad 4 de PROXIMOS_PASOS.md).
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ProveedorAutenticacion proveedor = ProveedorAutenticacion.LOCAL;

    // Reservado para un futuro login federado (ej. "sub" de un proveedor
    // OAuth externo); sin uso activo hoy.
    @Column(name = "proveedor_id", length = 191)
    private String proveedorId;

    @Column(name = "email_verificado", nullable = false)
    @Builder.Default
    private Boolean emailVerificado = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();
}

