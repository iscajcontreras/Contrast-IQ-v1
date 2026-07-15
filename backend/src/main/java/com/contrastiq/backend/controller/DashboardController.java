package com.contrastiq.backend.controller;

import com.contrastiq.backend.dto.DashboardKpiDTO;
import com.contrastiq.backend.dto.DistribucionProtocoloDTO;
import com.contrastiq.backend.dto.VolumenDiarioDTO;
import com.contrastiq.backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// Alimenta las 4 tarjetas KPI y los 2 graficos de la parte superior del
// dashboard. Recibe un rango de fechas (por defecto: hoy) que corresponde
// al filtro "Rango" del mockup.
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/kpis")
    public DashboardKpiDTO kpis(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        LocalDate hoy = LocalDate.now();
        LocalDateTime inicio = (desde != null ? desde : hoy).atStartOfDay();
        LocalDateTime fin = (hasta != null ? hasta : hoy).atTime(23, 59, 59);
        return dashboardService.kpis(inicio, fin);
    }

    @GetMapping("/uso-contraste")
    public List<VolumenDiarioDTO> usoContraste(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return dashboardService.volumenDiario(desde.atStartOfDay(), hasta.atTime(23, 59, 59));
    }

    @GetMapping("/distribucion-protocolo")
    public List<DistribucionProtocoloDTO> distribucionProtocolo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return dashboardService.distribucionPorIdentificadorAnatomico(desde.atStartOfDay(), hasta.atTime(23, 59, 59));
    }
}
