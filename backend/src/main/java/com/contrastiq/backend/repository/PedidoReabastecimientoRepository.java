package com.contrastiq.backend.repository;

import com.contrastiq.backend.model.PedidoReabastecimiento;
import com.contrastiq.backend.model.enums.EstadoPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;

public interface PedidoReabastecimientoRepository
        extends JpaRepository<PedidoReabastecimiento, Long>, JpaSpecificationExecutor<PedidoReabastecimiento> {

    // Usado por el job automatico: evita crear un segundo pedido si ya
    // hay uno pendiente/enviado para la misma sede+agente.
    boolean existsBySede_IdAndAgente_IdAndEstadoIn(Long sedeId, Long agenteId, List<EstadoPedido> estados);
}
