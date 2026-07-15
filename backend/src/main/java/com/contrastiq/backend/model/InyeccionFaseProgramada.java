package com.contrastiq.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

// Fase tal como quedo configurada en el inyector antes de disparar
// ("Programado"), distinta de protocolo_fases (plantilla "Planeado") y
// de inyeccion_fases (lo realmente ejecutado "Real"). Ver
// migration_dashboard_paciente_v2.sql.
@Entity
@Table(name = "inyeccion_fases_programadas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InyeccionFaseProgramada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inyeccion_id", nullable = false)
    private Inyeccion inyeccion;

    @Column(name = "orden_fase", nullable = false)
    private Short ordenFase;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agente_id", nullable = false)
    private AgenteContraste agente;

    @Column(name = "velocidad_flujo_ml_s", nullable = false, precision = 5, scale = 2)
    private BigDecimal velocidadFlujoMlS;

    @Column(name = "volumen_ml", nullable = false, precision = 6, scale = 2)
    private BigDecimal volumenMl;
}
