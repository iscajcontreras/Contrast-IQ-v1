package com.contrastiq.backend.service;

import com.contrastiq.backend.dto.CalibracionProgramadaDTO;
import com.contrastiq.backend.dto.PrediccionFallaDTO;
import com.contrastiq.backend.model.Inyector;
import com.contrastiq.backend.model.MantenimientoInyector;
import com.contrastiq.backend.model.enums.EstadoInyector;
import com.contrastiq.backend.model.enums.TipoMantenimiento;
import com.contrastiq.backend.repository.InyeccionRepository;
import com.contrastiq.backend.repository.InyectorRepository;
import com.contrastiq.backend.repository.MantenimientoInyectorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

// "Mantenimiento predictivo": prediccion de falla por ciclos de uso, y
// calendario de calibracion automatizado a partir del historial de
// mantenimientos ya registrado.
@Service
@RequiredArgsConstructor
public class MantenimientoPredictivoService {

    // Umbral de ciclos (inyecciones) desde el ultimo mantenimiento a
    // partir del cual se considera "riesgo de falla". Referencia
    // razonable de uso intensivo de un inyector de contraste; no
    // sustituye la recomendacion oficial del fabricante.
    private static final long UMBRAL_CICLOS_RIESGO = 600;

    // Intervalo estandar entre calibraciones (dias). 365 = anual, el
    // intervalo tipico recomendado para este tipo de equipo.
    private static final long INTERVALO_CALIBRACION_DIAS = 365;

    private final InyectorRepository inyectorRepository;
    private final InyeccionRepository inyeccionRepository;
    private final MantenimientoInyectorRepository mantenimientoRepository;

    @Transactional(readOnly = true)
    public List<PrediccionFallaDTO> predicciones() {
        List<Inyector> inyectores = inyectorRepository.findAll().stream()
                .filter(i -> i.getEstado() == EstadoInyector.ACTIVO)
                .toList();

        return inyectores.stream().map(inv -> {
            LocalDate fechaBase = inv.getFechaUltimoMantenimiento();
            LocalDateTime desde = (fechaBase != null ? fechaBase : LocalDate.now().minusYears(5))
                    .atStartOfDay();

            long ciclos = inyeccionRepository.countByInyector_IdAndFechaHoraInicioAfter(inv.getId(), desde);
            int porcentaje = (int) Math.min(100, Math.round((ciclos * 100.0) / UMBRAL_CICLOS_RIESGO));
            long dias = fechaBase != null ? ChronoUnit.DAYS.between(fechaBase, LocalDate.now()) : -1;

            return PrediccionFallaDTO.builder()
                    .inyectorId(inv.getId())
                    .numeroSerie(inv.getNumeroSerie())
                    .sala(inv.getSala().getNombre())
                    .sede(inv.getSala().getSede().getNombre())
                    .estado(inv.getEstado().name())
                    .ciclosDesdeMantenimiento(ciclos)
                    .umbralCiclos(UMBRAL_CICLOS_RIESGO)
                    .porcentajeUso(porcentaje)
                    .riesgoFalla(ciclos >= UMBRAL_CICLOS_RIESGO)
                    .fechaUltimoMantenimiento(fechaBase)
                    .diasDesdeMantenimiento(dias)
                    .build();
        })
        // los que estan mas cerca de necesitar mantenimiento primero
        .sorted((a, b) -> Long.compare(b.getCiclosDesdeMantenimiento(), a.getCiclosDesdeMantenimiento()))
        .toList();
    }

    @Transactional(readOnly = true)
    public List<CalibracionProgramadaDTO> calendarioCalibracion() {
        List<Inyector> inyectores = inyectorRepository.findAll().stream()
                .filter(i -> i.getEstado() == EstadoInyector.ACTIVO)
                .toList();

        return inyectores.stream().map(inv -> {
            Optional<MantenimientoInyector> ultima = mantenimientoRepository
                    .findTopByInyector_IdAndTipoOrderByFechaInicioDesc(inv.getId(), TipoMantenimiento.CALIBRACION);

            LocalDate ultimaFecha = ultima.map(m -> m.getFechaInicio().toLocalDate()).orElse(null);
            LocalDate proxima = (ultimaFecha != null ? ultimaFecha : LocalDate.now())
                    .plusDays(INTERVALO_CALIBRACION_DIAS);
            long diasRestantes = ChronoUnit.DAYS.between(LocalDate.now(), proxima);

            return CalibracionProgramadaDTO.builder()
                    .inyectorId(inv.getId())
                    .numeroSerie(inv.getNumeroSerie())
                    .sala(inv.getSala().getNombre())
                    .sede(inv.getSala().getSede().getNombre())
                    .ultimaCalibracion(ultimaFecha)
                    .proximaCalibracion(proxima)
                    .diasRestantes(diasRestantes)
                    .vencida(diasRestantes < 0)
                    .build();
        })
        .sorted((a, b) -> a.getProximaCalibracion().compareTo(b.getProximaCalibracion()))
        .toList();
    }
}
