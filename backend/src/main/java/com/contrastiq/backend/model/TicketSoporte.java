package com.contrastiq.backend.model;

import com.contrastiq.backend.model.enums.EstadoTicket;
import com.contrastiq.backend.model.enums.PrioridadTicket;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// "Mantenimiento predictivo": ticket de soporte abierto con el
// fabricante (Bracco/ACIST) cuando un inyector requiere intervencion
// tecnica que el equipo de biomedica del hospital no puede resolver.
@Entity
@Table(name = "tickets_soporte")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketSoporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inyector_id", nullable = false)
    private Inyector inyector;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creado_por", nullable = false)
    private Usuario creadoPor;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(nullable = false, length = 2000)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PrioridadTicket prioridad = PrioridadTicket.MEDIA;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private EstadoTicket estado = EstadoTicket.ABIERTO;

    @Column(name = "numero_ticket_fabricante", length = 100)
    private String numeroTicketFabricante;

    @Column(name = "respuesta_fabricante", length = 2000)
    private String respuestaFabricante;

    @Column(name = "fecha_creacion", nullable = false)
    @Builder.Default
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;
}
