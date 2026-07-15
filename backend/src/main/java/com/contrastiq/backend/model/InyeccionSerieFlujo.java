package com.contrastiq.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

// Serie de tiempo de flujo (contraste y salina) de una inyeccion
// especifica, analoga a InyeccionSeriePresion pero para la nueva
// grafica de flujo vs. tiempo del detalle de inyeccion.
@Entity
@Table(name = "inyeccion_serie_flujo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InyeccionSerieFlujo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inyeccion_id", nullable = false)
    private Inyeccion inyeccion;

    @Column(name = "tiempo_seg", nullable = false, precision = 6, scale = 2)
    private BigDecimal tiempoSeg;

    @Column(name = "flujo_contraste_ml_s", nullable = false, precision = 6, scale = 2)
    private BigDecimal flujoContrasteMlS;

    @Column(name = "flujo_salina_ml_s", nullable = false, precision = 6, scale = 2)
    private BigDecimal flujoSalinaMlS;
}
