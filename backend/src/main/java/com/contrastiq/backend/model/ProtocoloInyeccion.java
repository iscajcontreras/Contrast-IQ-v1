package com.contrastiq.backend.model;

import com.contrastiq.backend.model.enums.FuenteProtocolo;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "protocolos_inyeccion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProtocoloInyeccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "identificador_anatomico_id", nullable = false)
    private IdentificadorAnatomico identificadorAnatomico;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private FuenteProtocolo fuente = FuenteProtocolo.PERSONALIZADO;

    @Column(name = "velocidad_flujo_min_ml_s", precision = 5, scale = 2)
    private BigDecimal velocidadFlujoMinMlS;

    @Column(name = "velocidad_flujo_max_ml_s", precision = 5, scale = 2)
    private BigDecimal velocidadFlujoMaxMlS;

    @Column(name = "volumen_default_ml", precision = 6, scale = 2)
    private BigDecimal volumenDefaultMl;

    @Column(name = "numero_fases_default", nullable = false)
    @Builder.Default
    private Short numeroFasesDefault = 1;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();
}
