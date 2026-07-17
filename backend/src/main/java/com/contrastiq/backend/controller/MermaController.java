package com.contrastiq.backend.controller;

import com.contrastiq.backend.dto.MermaInyeccionDTO;
import com.contrastiq.backend.dto.MermaPorInsumoDTO;
import com.contrastiq.backend.dto.MermaPorSedeDTO;
import com.contrastiq.backend.dto.MermaResumenDTO;
import com.contrastiq.backend.dto.filtro.FiltroInyeccionDTO;
import com.contrastiq.backend.security.RequierePermiso;
import com.contrastiq.backend.service.MermaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// Merma de insumos (contraste + solucion salina): pantalla nueva dentro
// del modulo de Insumos que responde a la observacion de un stakeholder
// en reunion -- "no vi en ningun lado donde se muestran las mermas" --
// con las 4 vistas pedidas: KPI agregado con tendencia, por sede, por
// tipo de insumo/marca, y detalle por inyeccion individual.
@RestController
@RequestMapping("/api/insumos/mermas")
@RequiredArgsConstructor
public class MermaController {

    private final MermaService mermaService;

    // Merma julio 2026: tarjeta nueva del dashboard "Inyecciones de
    // contraste" ("Resultados con estos filtros" -> ahora merma real,
    // filtrada igual que la tabla de abajo). Deliberadamente SIN
    // @RequierePermiso(modulo = "INSUMOS_MERMAS", ...): a diferencia de
    // los otros 4 endpoints de este controller (que son la pantalla
    // dedicada de Merma de insumos), este lo consume el dashboard de
    // Inyecciones, que hoy no exige ningun permiso de modulo especifico
    // (ver InyeccionController/DashboardController) -- gatearlo con
    // INSUMOS_MERMAS:VER le devolveria 403 a un rol que si puede ver el
    // dashboard de inyecciones pero no tiene ese permiso (ej. RADIOLOGO,
    // que no esta en el seed de accesos a INSUMOS_MERMAS).
    @GetMapping("/resumen-filtrado")
    public MermaResumenDTO resumenFiltrado(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin,
            @RequestParam(required = false) Long sedeId,
            @RequestParam(required = false) Long salaId,
            @RequestParam(required = false) Long agenteId,
            @RequestParam(required = false) Long identificadorAnatomicoId,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Boolean soloConAlertaEda) {
        FiltroInyeccionDTO filtro = FiltroInyeccionDTO.builder()
                .fechaInicio(fechaInicio)
                .fechaFin(fechaFin)
                .sedeId(sedeId)
                .salaId(salaId)
                .agenteId(agenteId)
                .identificadorAnatomicoId(identificadorAnatomicoId)
                .estado(estado)
                .soloConAlertaEda(soloConAlertaEda)
                .build();
        return mermaService.resumenConFiltros(filtro);
    }

    @GetMapping("/resumen")
    @RequierePermiso(modulo = "INSUMOS_MERMAS", permiso = "VER")
    public MermaResumenDTO resumen(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return mermaService.resumen(desde.atStartOfDay(), hasta.atTime(23, 59, 59));
    }

    @GetMapping("/por-sede")
    @RequierePermiso(modulo = "INSUMOS_MERMAS", permiso = "VER")
    public List<MermaPorSedeDTO> porSede(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return mermaService.porSede(desde.atStartOfDay(), hasta.atTime(23, 59, 59));
    }

    @GetMapping("/por-insumo")
    @RequierePermiso(modulo = "INSUMOS_MERMAS", permiso = "VER")
    public List<MermaPorInsumoDTO> porInsumo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return mermaService.porInsumo(desde.atStartOfDay(), hasta.atTime(23, 59, 59));
    }

    @GetMapping("/por-inyeccion")
    @RequierePermiso(modulo = "INSUMOS_MERMAS", permiso = "VER")
    public Page<MermaInyeccionDTO> porInyeccion(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = hasta.atTime(23, 59, 59);
        Pageable pageable = PageRequest.of(page, size);
        return mermaService.porInyeccion(inicio, fin, pageable);
    }
}
