package com.contrastiq.backend.controller;

import com.contrastiq.backend.dto.ActualizarTicketRequest;
import com.contrastiq.backend.dto.CrearTicketRequest;
import com.contrastiq.backend.dto.TicketSoporteDTO;
import com.contrastiq.backend.service.TicketSoporteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets-soporte")
@RequiredArgsConstructor
public class TicketSoporteController {

    private final TicketSoporteService service;

    @GetMapping
    public List<TicketSoporteDTO> listar() {
        return service.listar();
    }

    @GetMapping("/inyector/{inyectorId}")
    public List<TicketSoporteDTO> listarPorInyector(@PathVariable Long inyectorId) {
        return service.listarPorInyector(inyectorId);
    }

    @PostMapping
    public ResponseEntity<TicketSoporteDTO> crear(
            @Valid @RequestBody CrearTicketRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request, authentication));
    }

    @PatchMapping("/{id}")
    public TicketSoporteDTO actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarTicketRequest request) {
        return service.actualizar(id, request);
    }
}
