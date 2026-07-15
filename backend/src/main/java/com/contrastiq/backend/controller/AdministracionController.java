package com.contrastiq.backend.controller;

import com.contrastiq.backend.dto.MatrizCeldaDTO;
import com.contrastiq.backend.dto.ModuloDTO;
import com.contrastiq.backend.dto.OtorgarPermisoRequest;
import com.contrastiq.backend.dto.PermisoDTO;
import com.contrastiq.backend.dto.RolDTO;
import com.contrastiq.backend.security.RequierePermiso;
import com.contrastiq.backend.service.PermisoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Pantalla "Roles y permisos" dentro de Administracion de usuarios.
// Restringido a ADMIN por completo (@PreAuthorize a nivel de clase, igual
// que UsuarioController) -- @RequierePermiso se agrega ademas en las
// mutaciones para dejar el patron listo por si en el futuro se decide
// abrir "solo lectura" de esta pantalla a otro rol.
@RestController
@RequestMapping("/api/administracion")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdministracionController {

    private final PermisoService permisoService;

    @GetMapping("/modulos")
    public List<ModuloDTO> modulos() {
        return permisoService.listarModulos();
    }

    @GetMapping("/permisos")
    public List<PermisoDTO> permisos() {
        return permisoService.listarPermisos();
    }

    @GetMapping("/roles")
    public List<RolDTO> roles() {
        return permisoService.listarRoles();
    }

    @GetMapping("/roles/{rolId}/matriz")
    public List<MatrizCeldaDTO> matriz(@PathVariable Long rolId) {
        return permisoService.obtenerMatrizDeRol(rolId);
    }

    @PostMapping("/roles/{rolId}/matriz/otorgar")
    @RequierePermiso(modulo = "ADMINISTRACION", permiso = "EDITAR")
    public ResponseEntity<Void> otorgar(@PathVariable Long rolId, @Valid @RequestBody OtorgarPermisoRequest request) {
        permisoService.otorgar(rolId, request.getModuloId(), request.getPermisoId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/roles/{rolId}/matriz/revocar")
    @RequierePermiso(modulo = "ADMINISTRACION", permiso = "EDITAR")
    public ResponseEntity<Void> revocar(@PathVariable Long rolId,
                                         @RequestParam Long moduloId,
                                         @RequestParam Long permisoId) {
        permisoService.revocar(rolId, moduloId, permisoId);
        return ResponseEntity.noContent().build();
    }
}
