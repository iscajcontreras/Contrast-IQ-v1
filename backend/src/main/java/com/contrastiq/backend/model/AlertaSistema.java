package com.contrastiq.backend.model;

import com.contrastiq.backend.model.enums.Severidad;
import com.contrastiq.backend.model.enums.TipoAlerta;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alertas_sistema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertaSistema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoAlerta tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Severidad severidad = Severidad.INFO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inyector_id")
    private Inyector inyector;

    @Column(nullable = false, length = 500)
    private String mensaje;

    @Column(name = "fecha_hora", nullable = false)
    @Builder.Default
    private LocalDateTime fechaHora = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private Boolean resuelta = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resuelta_por")
    private Usuario resueltaPor;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;
}
