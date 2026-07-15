package com.contrastiq.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

// Serie de tiempo de presion de una inyeccion especifica, usada para la
// grafica que se despliega al hacer clic en "Max. PSI" en la tabla de
// inyecciones (ver manual IRiS: "Si se selecciona este valor, se
// mostrara una grafica de toda la inyeccion de agente de contraste").
@Entity
@Table(name = "inyeccion_serie_presion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InyeccionSeriePresion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inyeccion_id", nullable = false)
    private Inyeccion inyeccion;

    @Column(name = "tiempo_seg", nullable = false, precision = 6, scale = 2)
    private BigDecimal tiempoSeg;

    @Column(name = "presion_psi", nullable = false, precision = 6, scale = 2)
    private BigDecimal presionPsi;
}
