package com.contrastiq.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "etiquetas_impresas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EtiquetaImpresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inyeccion_id", nullable = false)
    private Inyeccion inyeccion;

    @Column(name = "fecha_hora_impresion", nullable = false)
    @Builder.Default
    private LocalDateTime fechaHoraImpresion = LocalDateTime.now();

    @Column(length = 500)
    private String contenido;
}
