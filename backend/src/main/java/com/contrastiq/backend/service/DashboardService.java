package com.contrastiq.backend.service;

import com.contrastiq.backend.dto.DashboardKpiDTO;
import com.contrastiq.backend.dto.DistribucionProtocoloDTO;
import com.contrastiq.backend.dto.VolumenDiarioDTO;
import com.contrastiq.backend.model.enums.EstadoInyector;
import com.contrastiq.backend.repository.EventoExtravasacionRepository;
import com.contrastiq.backend.repository.InyeccionRepository;
import com.contrastiq.backend.repository.InyectorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDateTime;
import java.util.List;

// Alimenta las tarjetas KPI y los dos graficos de la parte superior del
// dashboard (uso de contraste por dia, distribucion por identificador
// anatomico). Recibe siempre un rango de fechas porque son vistas
// agregadas, no la lista filtrada fila por fila (eso lo cubre InyeccionService).
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final InyeccionRepository inyeccionRepository;
    private final InyectorRepository inyectorRepository;
    private final EventoExtravasacionRepository eventoExtravasacionRepository;

    public DashboardKpiDTO kpis(LocalDateTime desde, LocalDateTime hasta) {
        long totalInyecciones = inyeccionRepository.countByFechaHoraInicioBetween(desde, hasta);
        BigDecimal volumenTotal = inyeccionRepository.sumarVolumenEntreFechas(desde, hasta);
        BigDecimal promedio = totalInyecciones > 0
                ? volumenTotal.divide(BigDecimal.valueOf(totalInyecciones), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        long inyectoresActivos = inyectorRepository.countByEstado(EstadoInyector.ACTIVO);
        long inyectoresTotales = inyectorRepository.count();

        long alertasFueraDeRango = eventoExtravasacionRepository.count(
                (root, query, cb) -> cb.and(
                        cb.equal(root.get("estadoEda"), com.contrastiq.backend.model.enums.EstadoEda.FUERA_DE_RANGO),
                        cb.between(root.get("fechaHora"), desde, hasta)
                ));

        return DashboardKpiDTO.builder()
                .inyeccionesEnPeriodo(totalInyecciones)
                .volumenTotalMl(volumenTotal)
                .volumenPromedioMl(promedio)
                .alertasEdaFueraDeRango(alertasFueraDeRango)
                .inyectoresActivos(inyectoresActivos)
                .inyectoresTotales(inyectoresTotales)
                .build();
    }

    public List<VolumenDiarioDTO> volumenDiario(LocalDateTime desde, LocalDateTime hasta) {
        return inyeccionRepository.volumenDiarioEntreFechas(desde, hasta).stream()
                .map(fila -> VolumenDiarioDTO.builder()
                        .fecha(((Date) fila[0]).toLocalDate())
                        .volumenMl((BigDecimal) fila[1])
                        .totalInyecciones((Long) fila[2])
                        .build())
                .toList();
    }

    public List<DistribucionProtocoloDTO> distribucionPorIdentificadorAnatomico(
            LocalDateTime desde, LocalDateTime hasta) {
        List<Object[]> filas = inyeccionRepository.distribucionPorIdentificadorAnatomico(desde, hasta);
        long total = filas.stream().mapToLong(f -> (Long) f[1]).sum();

        return filas.stream()
                .map(fila -> {
                    long cantidad = (Long) fila[1];
                    double porcentaje = total > 0 ? (cantidad * 100.0 / total) : 0.0;
                    return DistribucionProtocoloDTO.builder()
                            .identificadorAnatomico((String) fila[0])
                            .total(cantidad)
                            .porcentaje(Math.round(porcentaje * 10.0) / 10.0)
                            .build();
                })
                .toList();
    }
}
