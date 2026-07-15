package com.contrastiq.backend.controller;

import com.contrastiq.backend.dto.ActualizarDosisRadiacionRequest;
import com.contrastiq.backend.dto.InyeccionDetalleCompletoResponse;
import com.contrastiq.backend.dto.InyeccionResumenDTO;
import com.contrastiq.backend.dto.PuntoPresionDTO;
import com.contrastiq.backend.dto.filtro.FiltroInyeccionDTO;
import com.contrastiq.backend.service.InyeccionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

// Endpoint principal de la tabla "Inyecciones recientes" del dashboard.
// Cada parametro de la barra de filtros del mockup tiene su equivalente
// como query param aqui; todos son opcionales.
//
// Ejemplo:
// GET /api/inyecciones?fechaInicio=2026-07-01T00:00:00&fechaFin=2026-07-03T23:59:59
//     &salaId=2&protocoloId=5&estado=COMPLETADA&page=0&size=20
@RestController
@RequestMapping("/api/inyecciones")
@RequiredArgsConstructor
public class InyeccionController {

    private final InyeccionService inyeccionService;

    @GetMapping
    public Page<InyeccionResumenDTO> buscar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin,
            @RequestParam(required = false) Long sedeId,
            @RequestParam(required = false) Long salaId,
            @RequestParam(required = false) Long inyectorId,
            @RequestParam(required = false) Long protocoloId,
            @RequestParam(required = false) Long identificadorAnatomicoId,
            @RequestParam(required = false) Long agenteId,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Boolean soloConAlertaEda,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        FiltroInyeccionDTO filtro = FiltroInyeccionDTO.builder()
                .fechaInicio(fechaInicio)
                .fechaFin(fechaFin)
                .sedeId(sedeId)
                .salaId(salaId)
                .inyectorId(inyectorId)
                .protocoloId(protocoloId)
                .identificadorAnatomicoId(identificadorAnatomicoId)
                .agenteId(agenteId)
                .estado(estado)
                .soloConAlertaEda(soloConAlertaEda)
                .build();

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fechaHoraInicio"));
        return inyeccionService.buscar(filtro, pageable);
    }

    // Grafica de presion vs. tiempo de una inyeccion (boton "Ver presion"
    // en la tabla, activo cuando tieneSeriePresion=true).
    @GetMapping("/{id}/presion")
    public List<PuntoPresionDTO> obtenerSeriePresion(@PathVariable Long id) {
        return inyeccionService.obtenerSeriePresion(id);
    }

    // "Dosis de radiacion combinada": registro manual de CTDIvol/DLP del
    // estudio de TAC asociado (viene del equipo de TAC, no del inyector).
    @PatchMapping("/{id}/dosis-radiacion")
    public InyeccionResumenDTO actualizarDosisRadiacion(
            @PathVariable Long id, @Valid @RequestBody ActualizarDosisRadiacionRequest request) {
        return inyeccionService.actualizarDosisRadiacion(id, request);
    }

    // Pantalla de detalle completo de una inyeccion: demograficos del
    // paciente, agente/lote, metadatos clinicos/operativos, grafica de
    // presion, grafica de flujo y el comparativo de fases Planeado /
    // Programado / Real.
    @GetMapping("/{id}/detalle-completo")
    public InyeccionDetalleCompletoResponse obtenerDetalleCompleto(@PathVariable Long id) {
        return inyeccionService.obtenerDetalleCompleto(id);
    }
}
