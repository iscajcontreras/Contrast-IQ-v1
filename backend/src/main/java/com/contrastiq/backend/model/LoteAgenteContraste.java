package com.contrastiq.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "lotes_agente_contraste",
       uniqueConstraints = @UniqueConstraint(columnNames = {"numero_lote", "agente_id", "sede_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoteAgenteContraste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agente_id", nullable = false)
    private AgenteContraste agente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @Column(name = "numero_lote", nullable = false, length = 100)
    private String numeroLote;

    @Column(name = "fecha_caducidad", nullable = false)
    private LocalDate fechaCaducidad;

    @Column(name = "cantidad_ml", nullable = false, precision = 9, scale = 2)
    private BigDecimal cantidadMl;

    @Column(name = "recibido_en", nullable = false)
    @Builder.Default
    private LocalDateTime recibidoEn = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
}
