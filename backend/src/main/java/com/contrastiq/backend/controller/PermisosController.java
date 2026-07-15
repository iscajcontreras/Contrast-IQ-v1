package com.contrastiq.backend.controller;

import com.contrastiq.backend.dto.PermisoModuloDTO;
import com.contrastiq.backend.model.Usuario;
import com.contrastiq.backend.repository.UsuarioRepository;
import com.contrastiq.backend.service.PermisoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Cualquier usuario autenticado (sin importar su rol) puede consultar
// SUS PROPIOS permisos -- esto alimenta PermisosService en Angular, que
// filtra el menu y arma permisoGuard con esta lista.
@RestController
@RequiredArgsConstructor
public class PermisosController {

    private final PermisoService permisoService;
    private final UsuarioRepository usuarioRepository;

    @GetMapping("/api/me/permisos")
    @Transactional(readOnly = true)
    public List<PermisoModuloDTO> misPermisos(@AuthenticationPrincipal Jwt jwt) {
        Usuario usuario = usuarioRepository.findByEmailConRol(jwt.getSubject())
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado para el token actual"));
        return permisoService.permisosDelRol(usuario.getRol().getNombre());
    }
}
