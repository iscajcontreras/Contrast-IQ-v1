package com.contrastiq.backend.model;

import com.contrastiq.backend.model.enums.EstadoLote;
import com.contrastiq.backend.model.enums.FuenteLote;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lotes_sincronizacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoteSincronizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FuenteLote fuente;

    @Column(name = "fecha_hora", nullable = false)
    @Builder.Default
    private LocalDateTime fechaHora = LocalDateTime.now();

    @Column(name = "registros_importados", nullable = false)
    @Builder.Default
    private Integer registrosImportados = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoLote estado = EstadoLote.EXITOSO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Lob
    private String detalle;
}
