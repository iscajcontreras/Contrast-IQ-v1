package com.contrastiq.backend.model;

import com.contrastiq.backend.model.enums.EstadoInyeccion;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inyecciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inyeccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inyector_id", nullable = false)
    private Inyector inyector;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "protocolo_id")
    private ProtocoloInyeccion protocolo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "identificador_anatomico_id")
    private IdentificadorAnatomico identificadorAnatomico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id")
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operador_id")
    private Usuario operador;

    @Column(name = "fecha_hora_inicio", nullable = false)
    private LocalDateTime fechaHoraInicio;

    @Column(name = "fecha_hora_fin")
    private LocalDateTime fechaHoraFin;

    @Column(name = "duracion_seg")
    private Integer duracionSeg;

    // "Filled Volume": lo cargado en la jeringa al armar el sistema
    @Column(name = "volumen_cargado_ml", precision = 6, scale = 2)
    private BigDecimal volumenCargadoMl;

    // "Injected Volume": lo realmente inyectado al paciente
    @Column(name = "volumen_total_ml", precision = 6, scale = 2)
    private BigDecimal volumenTotalMl;

    // "Residual Volume": columna generada en BD (cargado - inyectado);
    // no se escribe desde la aplicacion, solo se lee.
    @Column(name = "volumen_residual_ml", precision = 6, scale = 2, insertable = false, updatable = false)
    private BigDecimal volumenResidualMl;

    // "Max. PSI"
    @Column(name = "presion_maxima_psi", precision = 6, scale = 2)
    private BigDecimal presionMaximaPsi;

    // "Avg. PSI"
    @Column(name = "presion_promedio_psi", precision = 6, scale = 2)
    private BigDecimal presionPromedioPsi;

    // "Pressure Limit psi" configurado para esta inyeccion especifica
    @Column(name = "presion_limite_psi", precision = 6, scale = 2)
    private BigDecimal presionLimitePsi;

    // "EDA Enab." / lo contrario de "EDA User Disabled Injections"
    @Column(name = "eda_habilitado", nullable = false)
    @Builder.Default
    private Boolean edaHabilitado = true;

    // Para el conteo de "Syringes" (cantidad de jeringas usadas en el periodo)
    @Column(name = "jeringa_nueva", nullable = false)
    @Builder.Default
    private Boolean jeringaNueva = false;

    // "Dosis de radiacion combinada": metricas estandar de dosis del
    // estudio de TAC asociado a esta inyeccion (vienen del equipo de TAC,
    // no del inyector -- se registran aqui para verlas junto con el
    // volumen de contraste en el mismo lugar).
    @Column(name = "ctdi_vol_mgy", precision = 8, scale = 2)
    private BigDecimal ctdiVolMgy;

    @Column(name = "dlp_mgy_cm", precision = 10, scale = 2)
    private BigDecimal dlpMgyCm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoInyeccion estado;

    @Column(name = "motivo_aborto", length = 255)
    private String motivoAborto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_sincronizacion_id")
    private LoteSincronizacion loteSincronizacion;

    @Column(name = "creado_en", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();

    @OneToMany(mappedBy = "inyeccion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InyeccionFase> fases = new ArrayList<>();

    @OneToMany(mappedBy = "inyeccion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EventoExtravasacion> eventosExtravasacion = new ArrayList<>();

    @OneToMany(mappedBy = "inyeccion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("tiempoSeg ASC")
    @Builder.Default
    private List<InyeccionSeriePresion> seriePresion = new ArrayList<>();

    @OneToMany(mappedBy = "inyeccion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("tiempoSeg ASC")
    @Builder.Default
    private List<InyeccionSerieFlujo> serieFlujo = new ArrayList<>();

    @OneToMany(mappedBy = "inyeccion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordenFase ASC")
    @Builder.Default
    private List<InyeccionFaseProgramada> fasesProgramadas = new ArrayList<>();

    // Metadatos clinicos/operativos adicionales del detalle completo de
    // inyeccion (ver migration_dashboard_paciente_v2.sql).
    @Column(name = "numero_accesion", length = 64)
    private String numeroAccesion;

    @Column(name = "procedimiento_programado", length = 150)
    private String procedimientoProgramado;

    @Column(name = "calibre_aguja", length = 20)
    private String calibreAguja;

    @Column(name = "acceso_aguja", length = 50)
    private String accesoAguja;

    @Column(name = "avance_salina_ml", precision = 6, scale = 2)
    private BigDecimal avanceSalinaMl;

    @Column(name = "salina_jump_usado", nullable = false)
    @Builder.Default
    private Boolean salinaJumpUsado = false;

    @Column(length = 150)
    private String scanner;

    @Column(columnDefinition = "TEXT")
    private String notas;

    @Column(name = "dosis_contraste_gl", precision = 6, scale = 2)
    private BigDecimal dosisContrasteGl;

    @Column(name = "retraso_escaneo_seg")
    private Integer retrasoEscaneoSeg;
}
