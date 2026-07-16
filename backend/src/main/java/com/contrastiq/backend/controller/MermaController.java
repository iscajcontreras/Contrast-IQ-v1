package com.contrastiq.backend.controller;

import com.contrastiq.backend.dto.MermaInyeccionDTO;
import com.contrastiq.backend.dto.MermaPorInsumoDTO;
import com.contrastiq.backend.dto.MermaPorSedeDTO;
import com.contrastiq.backend.dto.MermaResumenDTO;
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
