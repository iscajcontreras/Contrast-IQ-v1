package com.contrastiq.backend.controller;

import com.contrastiq.backend.dto.HistorialAccesoDTO;
import com.contrastiq.backend.service.HistorialAccesoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// Historial de accesos: solo ADMIN puede ver el historial de CUALQUIER
// usuario; ver el propio historial no requiere ese rol (por eso el
// metodo /me esta disponible para cualquiera autenticado).
@RestController
@RequestMapping("/api/historial-accesos")
@RequiredArgsConstructor
public class HistorialAccesoController {

    private final HistorialAccesoService historialService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<HistorialAccesoDTO> listarTodos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return historialService.listarTodos(PageRequest.of(page, size));
    }

    @GetMapping("/usuario/{usuarioId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<HistorialAccesoDTO> listarPorUsuario(
            @PathVariable Long usuarioId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return historialService.listarPorUsuario(usuarioId, PageRequest.of(page, size));
    }
}
