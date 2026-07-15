package com.contrastiq.backend.controller;

import com.contrastiq.backend.dto.EventoExtravasacionDTO;
import com.contrastiq.backend.dto.RevisarExtravasacionRequest;
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
@RestController
@RequestMapping("/api/extravasaciones")
@RequiredArgsConstructor
public class ExtravasacionController {

    private final ExtravasacionService extravasacionService;

    @GetMapping
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
    public EventoExtravasacionDTO revisar(
            @PathVariable Long id,
            @Valid @RequestBody RevisarExtravasacionRequest request,
            Authentication authentication
    ) {
        return extravasacionService.revisar(id, request, authentication);
    }
}
