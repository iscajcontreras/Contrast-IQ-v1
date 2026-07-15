package com.contrastiq.backend.controller;

import com.contrastiq.backend.dto.ActualizarPedidoRequest;
import com.contrastiq.backend.dto.PedidoReabastecimientoDTO;
import com.contrastiq.backend.service.PedidoReabastecimientoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos-reabastecimiento")
@RequiredArgsConstructor
public class PedidoReabastecimientoController {

    private final PedidoReabastecimientoService service;

    @GetMapping
    public List<PedidoReabastecimientoDTO> listar() {
        return service.listar();
    }

    @PatchMapping("/{id}")
    public PedidoReabastecimientoDTO actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarPedidoRequest request) {
        return service.actualizar(id, request);
    }
}
