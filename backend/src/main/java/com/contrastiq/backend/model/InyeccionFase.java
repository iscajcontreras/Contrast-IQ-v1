package com.contrastiq.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "inyeccion_fases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InyeccionFase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inyeccion_id", nullable = false)
    private Inyeccion inyeccion;

    @Column(name = "numero_fase", nullable = false)
    private Short numeroFase;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agente_id", nullable = false)
    private AgenteContraste agente;

    // Nulo para fases historicas previas a la trazabilidad por lote, o
    // para fases de suero fisiologico que normalmente no se rastrean.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_agente_id")
    private LoteAgenteContraste loteAgente;

    @Column(name = "volumen_programado_ml", precision = 6, scale = 2)
    private BigDecimal volumenProgramadoMl;

    @Column(name = "volumen_real_ml", precision = 6, scale = 2)
    private BigDecimal volumenRealMl;

    @Column(name = "velocidad_flujo_ml_s", precision = 5, scale = 2)
    private BigDecimal velocidadFlujoMlS;

    @Column(name = "es_fase_contraste", nullable = false)
    @Builder.Default
    private Boolean esFaseContraste = true;
}
