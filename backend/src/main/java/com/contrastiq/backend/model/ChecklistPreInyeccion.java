package com.contrastiq.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Checklist de seguridad del paciente que un tecnico/radiologo completa
// (y firma digitalmente) antes de realizar una inyeccion de contraste.
// No esta atado a una fila de `inyecciones` porque ocurre ANTES de que
// el inyector siquiera arranque -- es un control de negocio, no un
// registro de lo que el equipo hizo.
@Entity
@Table(name = "checklist_pre_inyeccion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChecklistPreInyeccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sala_id")
    private Sala sala;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operador_id", nullable = false)
    private Usuario operador;

    @Column(name = "identidad_verificada", nullable = false)
    private Boolean identidadVerificada;

    @Column(name = "gfr_revisado", nullable = false)
    private Boolean gfrRevisado;

    // Snapshot: el GFR del paciente en el momento del checklist (aunque
    // el valor del paciente cambie despues, este queda congelado aqui)
    @Column(name = "gfr_valor_momento", precision = 6, scale = 2)
    private BigDecimal gfrValorMomento;

    @Column(name = "riesgo_renal_momento", nullable = false)
    private Boolean riesgoRenalMomento;

    @Column(name = "alergias_revisadas", nullable = false)
    private Boolean alergiasRevisadas;

    @Column(name = "alergias_momento", length = 500)
    private String alergiasMomento;

    @Column(name = "consentimiento_firmado", nullable = false)
    private Boolean consentimientoFirmado;

    @Column(length = 500)
    private String observaciones;

    @Column(name = "firma_nombre", nullable = false, length = 150)
    private String firmaNombre;

    // Trazo de la firma capturado en un <canvas> del frontend, como PNG
    // en base64. Es una firma digital "de trazo", no una firma
    // electronica avanzada con certificado -- suficiente para dejar
    // constancia de quien confirmo el checklist.
    @Lob
    @Column(name = "firma_imagen_base64", nullable = false, length = 100_000_000)
    private String firmaImagenBase64;

    @Column(name = "fecha_hora", nullable = false)
    @Builder.Default
    private LocalDateTime fechaHora = LocalDateTime.now();
}
