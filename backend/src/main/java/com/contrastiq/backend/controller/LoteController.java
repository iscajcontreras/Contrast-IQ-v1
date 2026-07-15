package com.contrastiq.backend.controller;

import com.contrastiq.backend.dto.CrearLoteRequest;
import com.contrastiq.backend.dto.LoteDTO;
import com.contrastiq.backend.dto.TrazabilidadLoteDTO;
import com.contrastiq.backend.service.LoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Trazabilidad de insumos: registro de lotes de contraste y, ante un
// recall, la lista de pacientes/inyecciones que usaron un lote dado.
@RestController
@RequestMapping("/api/lotes")
@RequiredArgsConstructor
public class LoteController {

    private final LoteService loteService;

    @GetMapping
    public Page<LoteDTO> buscar(
            @RequestParam(required = false) Long sedeId,
            @RequestParam(required = false) Long agenteId,
            @RequestParam(required = false) Boolean soloVigentes,
            @RequestParam(required = false) Boolean proximosACaducar,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "fechaCaducidad"));
        return loteService.buscar(sedeId, agenteId, soloVigentes, proximosACaducar, pageable);
    }

    @PostMapping
    public ResponseEntity<LoteDTO> crear(@Valid @RequestBody CrearLoteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loteService.crear(request));
    }

    @GetMapping("/{id}/trazabilidad")
    public List<TrazabilidadLoteDTO> trazabilidad(@PathVariable Long id) {
        return loteService.trazabilidad(id);
    }
}
