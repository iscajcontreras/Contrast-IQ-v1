package com.contrastiq.backend.service;

import com.contrastiq.backend.dto.MermaInyeccionDTO;
import com.contrastiq.backend.dto.MermaPorInsumoDTO;
import com.contrastiq.backend.dto.MermaPorSedeDTO;
import com.contrastiq.backend.dto.MermaResumenDTO;
import com.contrastiq.backend.repository.InyeccionFaseRepository;
import com.contrastiq.backend.util.ValidadorRangoFechas;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

// Merma de insumos (contraste + solucion salina): volumen programado vs.
// volumen realmente inyectado por fase (inyeccion_fases), agregado en 4
// vistas -- ver MermaController. Deliberadamente NO usa
// inyecciones.volumen_residual_ml (que es cargado - total, no programado -
// real) para que el KPI agregado, el desglose por sede, el desglose por
// insumo y el detalle por inyeccion compartan la misma fuente de verdad.
@Service
@RequiredArgsConstructor
public class MermaService {

    private final InyeccionFaseRepository inyeccionFaseRepository;

    public MermaResumenDTO resumen(LocalDateTime desde, LocalDateTime hasta) {
        ValidadorRangoFechas.validar(desde, hasta);
        BigDecimal[] actual = sumas(inyeccionFaseRepository.sumasEntreFechas(desde, hasta));
        BigDecimal programado = actual[0];
        BigDecimal real = actual[1];
        BigDecimal merma = programado.subtract(real);
        BigDecimal porcentaje = porcentaje(merma, programado);

        // Tendencia: mismo numero de dias, inmediatamente antes de "desde".
        Duration duracion = Duration.between(desde, hasta);
        LocalDateTime desdeAnterior = desde.minus(duracion).minusSeconds(1);
        LocalDateTime hastaAnterior = desde.minusSeconds(1);
        BigDecimal[] anterior = sumas(inyeccionFaseRepository.sumasEntreFechas(desdeAnterior, hastaAnterior));
        BigDecimal mermaAnterior = anterior[0].subtract(anterior[1]);

        BigDecimal variacion = null;
        if (mermaAnterior.compareTo(BigDecimal.ZERO) > 0) {
            variacion = merma.subtract(mermaAnterior)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(mermaAnterior, 1, RoundingMode.HALF_UP);
        }

        return new MermaResumenDTO(programado, real, merma, porcentaje, mermaAnterior, variacion);
    }

    public List<MermaPorSedeDTO> porSede(LocalDateTime desde, LocalDateTime hasta) {
        ValidadorRangoFechas.validar(desde, hasta);
        return inyeccionFaseRepository.sumasPorSede(desde, hasta).stream()
                .map(fila -> {
                    Long sedeId = (Long) fila[0];
                    String sede = (String) fila[1];
                    BigDecimal programado = (BigDecimal) fila[2];
                    BigDecimal real = (BigDecimal) fila[3];
                    BigDecimal merma = programado.subtract(real);
                    return new MermaPorSedeDTO(sedeId, sede, programado, real, merma, porcentaje(merma, programado));
                })
                .toList();
    }

    public List<MermaPorInsumoDTO> porInsumo(LocalDateTime desde, LocalDateTime hasta) {
        ValidadorRangoFechas.validar(desde, hasta);
        return inyeccionFaseRepository.sumasPorInsumo(desde, hasta).stream()
                .map(fila -> {
                    Long agenteId = (Long) fila[0];
                    String nombreComercial = (String) fila[1];
                    String tipo = fila[2].toString();
                    String fabricante = (String) fila[3];
                    BigDecimal programado = (BigDecimal) fila[4];
                    BigDecimal real = (BigDecimal) fila[5];
                    BigDecimal merma = programado.subtract(real);
                    return new MermaPorInsumoDTO(agenteId, nombreComercial, tipo, fabricante, programado, real,
                            merma, porcentaje(merma, programado));
                })
                .toList();
    }

    public Page<MermaInyeccionDTO> porInyeccion(LocalDateTime desde, LocalDateTime hasta, Pageable pageable) {
        ValidadorRangoFechas.validar(desde, hasta);
        return inyeccionFaseRepository.detallePorInyeccion(desde, hasta, pageable)
                .map(fila -> {
                    BigDecimal programado = (BigDecimal) fila[8];
                    BigDecimal real = (BigDecimal) fila[9];
                    BigDecimal merma = programado.subtract(real);
                    return new MermaInyeccionDTO(
                            (Long) fila[0],
                            (LocalDateTime) fila[1],
                            (String) fila[2],
                            (String) fila[3],
                            (String) fila[4],
                            (String) fila[5],
                            fila[6] != null ? fila[6].toString() : null,
                            (String) fila[7],
                            programado,
                            real,
                            merma,
                            porcentaje(merma, programado));
                });
    }

    // sumasEntreFechas siempre regresa exactamente 1 fila (agregado sin
    // GROUP BY -- coalesce garantiza 0 en vez de NULL aunque no haya
    // coincidencias), de ahi el .get(0) directo.
    private BigDecimal[] sumas(List<Object[]> filas) {
        Object[] fila = filas.get(0);
        return new BigDecimal[] { (BigDecimal) fila[0], (BigDecimal) fila[1] };
    }

    private BigDecimal porcentaje(BigDecimal merma, BigDecimal programado) {
        if (programado.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return merma.multiply(BigDecimal.valueOf(100)).divide(programado, 1, RoundingMode.HALF_UP);
    }
}
