package com.contrastiq.backend.model;

import com.contrastiq.backend.model.enums.ProveedorAutenticacion;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// Registro de cada intento de login (exitoso o fallido). Alimenta la
// pantalla "Historial de accesos" del modulo de gestion de usuarios.
@Entity
@Table(name = "historial_accesos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistorialAcceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nulo si el email ni siquiera corresponde a una cuenta existente
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "email_usado", nullable = false, length = 150)
    private String emailUsado;

    @Column(nullable = false)
    private Boolean exitoso;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ProveedorAutenticacion metodo = ProveedorAutenticacion.LOCAL;

    @Column(name = "ip_origen", length = 64)
    private String ipOrigen;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "fecha_hora", nullable = false)
    @Builder.Default
    private LocalDateTime fechaHora = LocalDateTime.now();
}
