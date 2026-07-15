package com.contrastiq.backend.service;

import com.contrastiq.backend.dto.ActualizarPedidoRequest;
import com.contrastiq.backend.dto.PedidoReabastecimientoDTO;
import com.contrastiq.backend.model.AgenteContraste;
import com.contrastiq.backend.model.PedidoReabastecimiento;
import com.contrastiq.backend.model.Sede;
import com.contrastiq.backend.model.enums.EstadoPedido;
import com.contrastiq.backend.repository.PedidoReabastecimientoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// "Reorden automatico de stock": el job de alertas crea aqui un pedido
// cuando detecta stock bajo (ver AlertasAutomaticasScheduler); farmacia
// le da seguimiento desde el frontend (enviado/recibido).
@Service
@RequiredArgsConstructor
public class PedidoReabastecimientoService {

    private final PedidoReabastecimientoRepository pedidoRepository;

    @Transactional(readOnly = true)
    public List<PedidoReabastecimientoDTO> listar() {
        return pedidoRepository.findAll().stream()
                .sorted((a, b) -> b.getFechaSolicitud().compareTo(a.getFechaSolicitud()))
                .map(this::aDto)
                .toList();
    }

    // Llamado por AlertasAutomaticasScheduler. Genera un pedido por el
    // doble del stock minimo menos lo que ya hay, para no quedar justo
    // en el limite otra vez apenas llegue el reabastecimiento.
    @Transactional
    public void generarAutomatico(Sede sede, AgenteContraste agente, BigDecimal stockActual, BigDecimal stockMinimo) {
        BigDecimal cantidad = stockMinimo.multiply(BigDecimal.valueOf(2)).subtract(stockActual);
        if (cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            cantidad = stockMinimo;
        }

        PedidoReabastecimiento pedido = PedidoReabastecimiento.builder()
                .sede(sede)
                .agente(agente)
                .cantidadSolicitadaMl(cantidad)
                .estado(EstadoPedido.PENDIENTE)
                .generadoAutomaticamente(true)
                .notas("Generado automaticamente: stock (" + stockActual + " ml) por debajo del minimo (" + stockMinimo + " ml)")
                .build();

        pedidoRepository.save(pedido);
    }

    @Transactional
    public PedidoReabastecimientoDTO actualizar(Long id, ActualizarPedidoRequest request) {
        PedidoReabastecimiento pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El pedido no existe"));

        EstadoPedido nuevoEstado = EstadoPedido.valueOf(request.getEstado());
        pedido.setEstado(nuevoEstado);
        if (nuevoEstado == EstadoPedido.ENVIADO) {
            pedido.setFechaEnvio(LocalDateTime.now());
        } else if (nuevoEstado == EstadoPedido.RECIBIDO) {
            pedido.setFechaRecepcion(LocalDateTime.now());
        }

        return aDto(pedidoRepository.save(pedido));
    }

    private PedidoReabastecimientoDTO aDto(PedidoReabastecimiento p) {
        return PedidoReabastecimientoDTO.builder()
                .id(p.getId())
                .sede(p.getSede().getNombre())
                .agente(p.getAgente().getNombreComercial())
                .cantidadSolicitadaMl(p.getCantidadSolicitadaMl())
                .estado(p.getEstado().name())
                .generadoAutomaticamente(p.getGeneradoAutomaticamente())
                .fechaSolicitud(p.getFechaSolicitud())
                .fechaEnvio(p.getFechaEnvio())
                .fechaRecepcion(p.getFechaRecepcion())
                .notas(p.getNotas())
                .build();
    }
}
