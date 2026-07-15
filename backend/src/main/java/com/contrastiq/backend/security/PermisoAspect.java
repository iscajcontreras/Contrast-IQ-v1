package com.contrastiq.backend.security;

import com.contrastiq.backend.model.Usuario;
import com.contrastiq.backend.repository.RolModuloPermisoRepository;
import com.contrastiq.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

// Enforcement server-side de @RequierePermiso, ademas (no en lugar) de
// @PreAuthorize hasRole('ADMIN') donde ya exista -- este aspecto cubre el
// caso mas fino de "ADMIN puede entrar al modulo, pero solo con permiso
// EDITAR puede modificar" en modulos que no son exclusivos de ADMIN.
//
// ADVERTENCIA IMPORTANTE (documentada en el proyecto de referencia
// CEROGAS GPS y replicada aqui a proposito): un metodo @Around de
// AspectJ se invoca via reflexion directa sobre el objeto real, NO a
// traves del proxy dinamico de Spring -- por eso @Transactional en el
// metodo del controller NO tiene efecto para lo que pase DENTRO de este
// aspecto. Si aqui se intentara acceder a una relacion LAZY de Usuario
// (por ejemplo usuario.getRol()) sin una sesion de Hibernate abierta,
// lanzaria LazyInitializationException de forma intermitente y dificil
// de diagnosticar. La solucion, aplicada abajo, es usar
// UsuarioRepository.findByEmailConRol() (JOIN FETCH), que trae el rol ya
// cargado en la misma consulta -- nunca depender de que quede abierta
// una sesion.
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class PermisoAspect {

    private final UsuarioRepository usuarioRepository;
    private final RolModuloPermisoRepository rolModuloPermisoRepository;

    @Around("@annotation(requierePermiso)")
    public Object verificarPermiso(ProceedingJoinPoint joinPoint, RequierePermiso requierePermiso) throws Throwable {
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                : null;

        if (!(principal instanceof Jwt jwt)) {
            throw new AccessDeniedException("No tienes permiso para realizar esta accion");
        }

        String email = jwt.getSubject();
        Usuario usuario = usuarioRepository.findByEmailConRol(email)
                .orElseThrow(() -> new AccessDeniedException("No tienes permiso para realizar esta accion"));

        boolean tienePermiso = rolModuloPermisoRepository.tienePermiso(
                usuario.getRol().getNombre(), requierePermiso.modulo(), requierePermiso.permiso());

        if (!tienePermiso) {
            log.warn("Acceso denegado: usuario {} (rol {}) sin permiso {}:{}",
                    email, usuario.getRol().getNombre(), requierePermiso.modulo(), requierePermiso.permiso());
            throw new AccessDeniedException("No tienes permiso para realizar esta accion");
        }

        return joinPoint.proceed();
    }
}
