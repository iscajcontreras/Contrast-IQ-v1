package com.contrastiq.backend.model;

import com.contrastiq.backend.model.enums.EstadoInyector;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inyectores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inyector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sala_id", nullable = false)
    private Sala sala;

    @Column(name = "numero_serie", nullable = false, unique = true, length = 80)
    private String numeroSerie;

    @Column(nullable = false, length = 100)
    @Builder.Default
    private String modelo = "EmpowerCTA";

    @Column(name = "fecha_instalacion")
    private LocalDate fechaInstalacion;

    @Column(name = "version_firmware", length = 50)
    private String versionFirmware;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoInyector estado = EstadoInyector.ACTIVO;

    @Column(name = "fecha_ultimo_mantenimiento")
    private LocalDate fechaUltimoMantenimiento;

    @Column(name = "creado_en", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Column(name = "actualizado_en", nullable = false)
    @Builder.Default
    private LocalDateTime actualizadoEn = LocalDateTime.now();
}
