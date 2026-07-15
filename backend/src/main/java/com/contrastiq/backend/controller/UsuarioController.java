package com.contrastiq.backend.controller;

import com.contrastiq.backend.dto.*;
import com.contrastiq.backend.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// Pantalla "Gestion de usuarios". Toda la funcionalidad (incluida la
// simple lectura) queda restringida al rol ADMIN: la lista de usuarios
// del sistema no es informacion que deba ver cualquier perfil.
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    public Page<UsuarioResumenDTO> buscar(
            @RequestParam(required = false) Long sedeId,
            @RequestParam(required = false) Long rolId,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) String busqueda,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "nombreCompleto"));
        return usuarioService.buscar(sedeId, rolId, activo, busqueda, pageable);
    }

    @GetMapping("/{id}")
    public UsuarioResumenDTO obtener(@PathVariable Long id) {
        return usuarioService.obtener(id);
    }

    @PostMapping
    public ResponseEntity<UsuarioResumenDTO> crear(@Valid @RequestBody CrearUsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crear(request));
    }

    @PutMapping("/{id}")
    public UsuarioResumenDTO actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarUsuarioRequest request) {
        return usuarioService.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    public UsuarioResumenDTO cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoUsuarioRequest request) {
        return usuarioService.cambiarEstado(id, request.getActivo());
    }
}
