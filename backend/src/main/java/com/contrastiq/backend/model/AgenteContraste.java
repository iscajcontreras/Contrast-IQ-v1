package com.contrastiq.backend.model;

import com.contrastiq.backend.model.enums.TipoAgente;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "agentes_contraste")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgenteContraste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_comercial", nullable = false, length = 100)
    private String nombreComercial;

    @Column(length = 30)
    private String concentracion;

    @Column(length = 100)
    private String fabricante;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TipoAgente tipo = TipoAgente.CONTRASTE;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
}
