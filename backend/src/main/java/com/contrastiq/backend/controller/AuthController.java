package com.contrastiq.backend.controller;

import com.contrastiq.backend.dto.LoginRequest;
import com.contrastiq.backend.dto.OlvidarPasswordRequest;
import com.contrastiq.backend.dto.RefrescarTokenRequest;
import com.contrastiq.backend.dto.RegistroRequest;
import com.contrastiq.backend.dto.RestablecerPasswordRequest;
import com.contrastiq.backend.dto.TokenResponse;
import com.contrastiq.backend.dto.UsuarioActualDTO;
import com.contrastiq.backend.model.Usuario;
import com.contrastiq.backend.repository.UsuarioRepository;
import com.contrastiq.backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// Login directo: el front manda email+password aqui y se lleva un access
// token (JWT, corta duracion) + un refresh token (opaco, vive mas
// tiempo) en la misma respuesta -- sin redirects, sin Authorization
// Server aparte, sin doble pantalla de login. Ver AuthService para la
// logica de emision/renovacion.
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UsuarioRepository usuarioRepository;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request,
                                                HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.login(request, httpRequest));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refrescar(@Valid @RequestBody RefrescarTokenRequest request) {
        return ResponseEntity.ok(authService.refrescar(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefrescarTokenRequest request) {
        authService.cerrarSesion(request);
        return ResponseEntity.noContent().build();
    }

    // Usado por la pantalla "Sign up" de Angular
    @PostMapping("/registro")
    public ResponseEntity<Map<String, String>> registrar(@Valid @RequestBody RegistroRequest request) {
        authService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("mensaje", "Cuenta creada correctamente. Ya puedes iniciar sesion."));
    }

    // Usado por la pantalla "Forgot password" de Angular
    @PostMapping("/olvidar-password")
    public ResponseEntity<Map<String, String>> olvidarPassword(@Valid @RequestBody OlvidarPasswordRequest request) {
        authService.solicitarRecuperacion(request);
        // Respuesta generica a proposito: no confirma si el correo existe
        return ResponseEntity.ok(Map.of(
                "mensaje", "Si el correo existe en nuestro sistema, te enviamos un enlace de recuperacion."));
    }

    // Usado por la pantalla "Reset password" de Angular (?token=... en la URL)
    @PostMapping("/restablecer-password")
    public ResponseEntity<Map<String, String>> restablecerPassword(
            @Valid @RequestBody RestablecerPasswordRequest request) {
        authService.restablecerPassword(request);
        return ResponseEntity.ok(Map.of("mensaje", "Tu contrasena fue actualizada. Ya puedes iniciar sesion."));
    }

    // Usado por el menu de usuario de Angular para mostrar el nombre real
    // de quien inicio sesion. El JWT lo emite este mismo backend
    // (JwtSecurityConfig); su claim "sub" siempre es el email.
    @GetMapping("/me")
    @Transactional(readOnly = true)
    public ResponseEntity<UsuarioActualDTO> me(@AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getSubject();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado para el token actual"));

        return ResponseEntity.ok(UsuarioActualDTO.builder()
                .nombreCompleto(usuario.getNombreCompleto())
                .email(usuario.getEmail())
                .rol(usuario.getRol().getNombre().name())
                .build());
    }
}
