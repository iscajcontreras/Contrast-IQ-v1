package com.contrastiq.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "pacientes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Paciente {

    public enum Sexo { M, F, OTRO, NO_ESPECIFICADO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "identificador_externo", nullable = false, unique = true, length = 100)
    private String identificadorExterno;

    // Solo para demos/pruebas -- ver migration_nombre_paciente.sql. En un
    // despliegue real, el nombre del paciente deberia vivir en el HIS/RIS,
    // no duplicarse aqui.
    @Column(name = "nombre_completo", length = 150)
    private String nombreCompleto;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private Sexo sexo = Sexo.NO_ESPECIFICADO;

    @Column(name = "peso_kg", precision = 5, scale = 2)
    private BigDecimal pesoKg;

    @Column(name = "gfr_ml_min", precision = 6, scale = 2)
    private BigDecimal gfrMlMin;

    // Valor de laboratorio real introducido por el usuario; el GFR de
    // arriba es el resultado calculado a partir de este valor (ver
    // manual IRiS: campos "Creatinine" y "GFR" son datos separados).
    @Column(name = "creatinina_mg_dl", precision = 5, scale = 2)
    private BigDecimal creatininaMgDl;

    // Alergias declaradas por el paciente (texto libre), revisadas
    // explicitamente en el checklist pre-inyeccion.
    @Column(length = 500)
    private String alergias;

    // "Integracion clinica": marca de la ultima vez que se trajeron
    // datos de este paciente desde el HIS (ver HisIntegracionService).
    @Column(name = "sincronizado_his_en")
    private LocalDateTime sincronizadoHisEn;

    @Column(name = "creado_en", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();

    // Demograficos adicionales para el detalle completo de inyeccion
    // (ver migration_dashboard_paciente_v2.sql).
    @Column(name = "numero_expediente", length = 64)
    private String numeroExpediente;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "talla_m", precision = 4, scale = 2)
    private BigDecimal tallaM;

    @Column(name = "grupo_etnico", length = 50)
    private String grupoEtnico;
}
