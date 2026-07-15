package com.contrastiq.backend.service;

import com.contrastiq.backend.dto.CrearAlertaRequest;
import com.contrastiq.backend.model.EventoExtravasacion;
import com.contrastiq.backend.model.InventarioAgente;
import com.contrastiq.backend.model.Inyector;
import com.contrastiq.backend.model.enums.EstadoEda;
import com.contrastiq.backend.model.enums.EstadoPedido;
import com.contrastiq.backend.model.enums.TipoAlerta;
import com.contrastiq.backend.repository.AlertaSistemaRepository;
import com.contrastiq.backend.repository.EventoExtravasacionRepository;
import com.contrastiq.backend.repository.InventarioAgenteRepository;
import com.contrastiq.backend.repository.InyectorRepository;
import com.contrastiq.backend.repository.PedidoReabastecimientoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

// "Alertas en tiempo real" (corto plazo): 3 jobs automaticos que
// generan una alerta (y la empujan por WebSocket via AlertaService)
// sin que nadie tenga que entrar a revisar manualmente:
//   1. EDA fuera de rango  -> push al radiologo (casi inmediato)
//   2. Mantenimiento vencido -> aviso a biomedica (diario)
//   3. Stock bajo de contraste -> aviso a farmacia (diario)
@Component
@RequiredArgsConstructor
@Slf4j
public class AlertasAutomaticasScheduler {

    private static final int DIAS_MANTENIMIENTO_VENCIDO = 90;

    private final EventoExtravasacionRepository eventoExtravasacionRepository;
    private final InyectorRepository inyectorRepository;
    private final InventarioAgenteRepository inventarioAgenteRepository;
    private final AlertaSistemaRepository alertaSistemaRepository;
    private final AlertaService alertaService;
    private final PedidoReabastecimientoService pedidoReabastecimientoService;
    private final PedidoReabastecimientoRepository pedidoReabastecimientoRepository;

    // Se ejecutan tambien una vez al arrancar la aplicacion, para que
    // una demostracion no tenga que esperar hasta la corrida programada
    // del dia para ver las alertas generarse.
    //
    // IMPORTANTE: este metodo necesita su propio @Transactional. Como
    // llama a los otros 3 con "this." (auto-invocacion dentro de la
    // misma clase), el proxy de Spring que aplica @Transactional se
    // saltaria por completo si este metodo no abriera ya una
    // transaccion -- por eso fallaba con LazyInitializationException
    // al arrancar, aunque cada metodo individual ya tenia @Transactional.
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void alArrancar() {
        log.info("Ejecutando revision inicial de alertas automaticas...");
        revisarEdaFueraDeRango();
        revisarMantenimientoVencido();
        revisarStockBajo();
    }

    // Cada minuto: lo mas cercano a "tiempo real" que permite un job
    // programado sin acoplarse directo al punto de insercion del dato.
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void revisarEdaFueraDeRango() {
        List<EventoExtravasacion> pendientes =
                eventoExtravasacionRepository.findByEstadoEdaAndAlertaGeneradaFalse(EstadoEda.FUERA_DE_RANGO);

        for (EventoExtravasacion evento : pendientes) {
            Inyector inyector = evento.getInyeccion().getInyector();

            CrearAlertaRequest request = new CrearAlertaRequest();
            request.setTipo(TipoAlerta.EDA_FUERA_DE_RANGO.name());
            request.setSeveridad("CRITICA");
            request.setInyectorId(inyector.getId());
            request.setMensaje("Posible extravasacion (EDA fuera de rango) en " + inyector.getSala().getNombre()
                    + " - inyector " + inyector.getNumeroSerie() + ". Requiere revision del radiologo.");
            alertaService.crear(request);

            evento.setAlertaGenerada(true);
        }

        if (!pendientes.isEmpty()) {
            log.info("Alertas EDA fuera de rango generadas: {}", pendientes.size());
        }
    }

    // Diario a las 7:00 am, mas la corrida inicial al arrancar.
    @Scheduled(cron = "0 0 7 * * *")
    @Transactional
    public void revisarMantenimientoVencido() {
        LocalDate fechaLimite = LocalDate.now().minusDays(DIAS_MANTENIMIENTO_VENCIDO);
        List<Inyector> vencidos = inyectorRepository.conMantenimientoVencido(fechaLimite);

        for (Inyector inyector : vencidos) {
            boolean yaExiste = alertaSistemaRepository
                    .existsByTipoAndInyector_IdAndResueltaFalse(TipoAlerta.EQUIPO_MANTENIMIENTO, inyector.getId());
            if (yaExiste) continue;

            CrearAlertaRequest request = new CrearAlertaRequest();
            request.setTipo(TipoAlerta.EQUIPO_MANTENIMIENTO.name());
            request.setSeveridad("ADVERTENCIA");
            request.setInyectorId(inyector.getId());
            request.setMensaje("El inyector " + inyector.getNumeroSerie() + " (" + inyector.getSala().getNombre()
                    + ") no registra mantenimiento hace mas de " + DIAS_MANTENIMIENTO_VENCIDO + " dias.");
            alertaService.crear(request);
        }

        if (!vencidos.isEmpty()) {
            log.info("Alertas de mantenimiento vencido generadas: {}", vencidos.size());
        }
    }

    // Diario a las 7:05 am, mas la corrida inicial al arrancar.
    @Scheduled(cron = "0 5 7 * * *")
    @Transactional
    public void revisarStockBajo() {
        List<InventarioAgente> bajos = inventarioAgenteRepository.conStockBajo();

        for (InventarioAgente inv : bajos) {
            String marcador = inv.getSede().getNombre() + " - " + inv.getAgente().getNombreComercial();
            boolean yaExisteAlerta = alertaSistemaRepository
                    .existsByTipoAndMensajeContainingAndResueltaFalse(TipoAlerta.STOCK_BAJO, marcador);
            if (!yaExisteAlerta) {
                CrearAlertaRequest request = new CrearAlertaRequest();
                request.setTipo(TipoAlerta.STOCK_BAJO.name());
                request.setSeveridad("ADVERTENCIA");
                request.setInyectorId(null);
                request.setMensaje("Stock bajo de " + marcador + ": " + inv.getStockMl()
                        + " ml disponibles (minimo " + inv.getStockMinimoMl() + " ml). Notificar a farmacia.");
                alertaService.crear(request);
            }

            // "Reorden automatico de stock": solo si no hay ya un pedido
            // pendiente/enviado para esta misma sede+agente.
            boolean yaExistePedido = pedidoReabastecimientoRepository.existsBySede_IdAndAgente_IdAndEstadoIn(
                    inv.getSede().getId(), inv.getAgente().getId(),
                    List.of(EstadoPedido.PENDIENTE, EstadoPedido.ENVIADO));
            if (!yaExistePedido) {
                pedidoReabastecimientoService.generarAutomatico(
                        inv.getSede(), inv.getAgente(), inv.getStockMl(), inv.getStockMinimoMl());
            }
        }

        if (!bajos.isEmpty()) {
            log.info("Alertas de stock bajo generadas: {}", bajos.size());
        }
    }
}
