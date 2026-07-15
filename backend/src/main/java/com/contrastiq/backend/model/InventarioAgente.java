package com.contrastiq.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventario_agentes",
       uniqueConstraints = @UniqueConstraint(columnNames = {"sede_id", "agente_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventarioAgente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agente_id", nullable = false)
    private AgenteContraste agente;

    @Column(name = "stock_ml", nullable = false, precision = 9, scale = 2)
    @Builder.Default
    private BigDecimal stockMl = BigDecimal.ZERO;

    @Column(name = "stock_minimo_ml", nullable = false, precision = 9, scale = 2)
    @Builder.Default
    private BigDecimal stockMinimoMl = BigDecimal.ZERO;

    @Column(name = "actualizado_en", nullable = false)
    @Builder.Default
    private LocalDateTime actualizadoEn = LocalDateTime.now();
}
