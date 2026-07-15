package com.contrastiq.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "protocolo_fases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProtocoloFase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "protocolo_id", nullable = false)
    private ProtocoloInyeccion protocolo;

    @Column(name = "numero_fase", nullable = false)
    private Short numeroFase;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agente_id", nullable = false)
    private AgenteContraste agente;

    @Column(name = "volumen_ml", nullable = false, precision = 6, scale = 2)
    private BigDecimal volumenMl;

    @Column(name = "velocidad_flujo_ml_s", nullable = false, precision = 5, scale = 2)
    private BigDecimal velocidadFlujoMlS;

    @Column(name = "es_fase_contraste", nullable = false)
    @Builder.Default
    private Boolean esFaseContraste = true;
}
