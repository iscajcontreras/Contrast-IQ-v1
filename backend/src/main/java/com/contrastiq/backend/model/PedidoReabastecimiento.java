package com.contrastiq.backend.model;

import com.contrastiq.backend.model.enums.EstadoPedido;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// "Trazabilidad de insumos": reorden de stock, generado automaticamente
// por el job de alertas cuando el inventario cae por debajo del minimo,
// o creado manualmente por farmacia.
@Entity
@Table(name = "pedidos_reabastecimiento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedidoReabastecimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agente_id", nullable = false)
    private AgenteContraste agente;

    @Column(name = "cantidad_solicitada_ml", nullable = false, precision = 9, scale = 2)
    private BigDecimal cantidadSolicitadaMl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoPedido estado = EstadoPedido.PENDIENTE;

    @Column(name = "generado_automaticamente", nullable = false)
    @Builder.Default
    private Boolean generadoAutomaticamente = false;

    @Column(name = "fecha_solicitud", nullable = false)
    @Builder.Default
    private LocalDateTime fechaSolicitud = LocalDateTime.now();

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    @Column(name = "fecha_recepcion")
    private LocalDateTime fechaRecepcion;

    @Column(length = 500)
    private String notas;
}
