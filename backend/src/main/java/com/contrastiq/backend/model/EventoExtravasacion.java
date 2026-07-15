package com.contrastiq.backend.model;

import com.contrastiq.backend.model.enums.EstadoEda;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "eventos_extravasacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventoExtravasacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inyeccion_id", nullable = false)
    private Inyeccion inyeccion;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_eda", nullable = false, length = 20)
    private EstadoEda estadoEda;

    @Column(nullable = false)
    @Builder.Default
    private Boolean revisado = false;

    // Evita que el job programado de alertas en tiempo real genere un
    // push duplicado cada vez que corre para el mismo evento.
    @Column(name = "alerta_generada", nullable = false)
    @Builder.Default
    private Boolean alertaGenerada = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revisado_por")
    private Usuario revisadoPor;

    @Column(name = "fecha_revision")
    private LocalDateTime fechaRevision;

    @Column(name = "accion_tomada", length = 255)
    private String accionTomada;

    @Lob
    private String notas;
}
