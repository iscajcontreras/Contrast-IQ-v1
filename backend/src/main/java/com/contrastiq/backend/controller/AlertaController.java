package com.contrastiq.backend.controller;

import com.contrastiq.backend.dto.AlertaDTO;
import com.contrastiq.backend.dto.CrearAlertaRequest;
import com.contrastiq.backend.service.AlertaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Alertas del sistema (mantenimiento, stock, comunicacion, EDA fuera de
// rango). El listado cubre lo que ya paso; /topic/alertas (WebSocket)
// cubre lo que pasa mientras el usuario tiene el dashboard abierto.
@RestController
@RequestMapping("/api/alertas")
@RequiredArgsConstructor
public class AlertaController {

    private final AlertaService alertaService;

    @GetMapping
    public Page<AlertaDTO> buscar(
            @RequestParam(required = false) Boolean resuelta,
            @RequestParam(required = false) String severidad,
            @RequestParam(required = false) String tipo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fechaHora"));
        return alertaService.buscar(resuelta, severidad, tipo, pageable);
    }

    @PostMapping
    public ResponseEntity<AlertaDTO> crear(@Valid @RequestBody CrearAlertaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(alertaService.crear(request));
    }

    @PatchMapping("/{id}/resolver")
    public AlertaDTO resolver(@PathVariable Long id) {
        return alertaService.resolver(id);
    }
}
