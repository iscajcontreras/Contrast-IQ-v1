package com.contrastiq.backend.controller;

import com.contrastiq.backend.dto.EventoExtravasacionDTO;
import com.contrastiq.backend.dto.RevisarExtravasacionRequest;
import com.contrastiq.backend.security.RequierePermiso;
import com.contrastiq.backend.service.ExtravasacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

// Cubre el filtro "Estado: Solo alertas EDA" del dashboard, mostrando el
// detalle de cada evento de extravasacion para su revision clinica.
//
// Julio 2026: hasta ahora este controller no tenia ningun
// @RequierePermiso (cualquier usuario autenticado podia listar Y marcar
// como revisado cualquier evento, sin pasar por la matriz Rol x Modulo x
// Permiso) porque nunca existio una pantalla que lo consumiera. Al
// construir la pantalla "Alertas de extravasacion" se agrego el modulo
// EXTRAVASACIONES (ver migration_modulo_extravasaciones.sql) y se gatean
// aqui sus 2 acciones, igual que el resto de controllers del sistema.
@RestController
@RequestMapping("/api/extravasaciones")
@RequiredArgsConstructor
public class ExtravasacionController {

    private final ExtravasacionService extravasacionService;

    @GetMapping
    @RequierePermiso(modulo = "EXTRAVASACIONES", permiso = "VER")
    public Page<EventoExtravasacionDTO> buscar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(required = false) String estadoEda,
            @RequestParam(required = false) Boolean revisado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fechaHora"));
        return extravasacionService.buscar(desde, hasta, estadoEda, revisado, pageable);
    }

    @PatchMapping("/{id}/revisar")
    @RequierePermiso(modulo = "EXTRAVASACIONES", permiso = "EDITAR")
    public EventoExtravasacionDTO revisar(
            @PathVariable Long id,
            @Valid @RequestBody RevisarExtravasacionRequest request,
            Authentication authentication
    ) {
        return extravasacionService.revisar(id, request, authentication);
    }
}
