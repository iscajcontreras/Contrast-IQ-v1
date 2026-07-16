package com.contrastiq.backend.security;

import com.contrastiq.backend.model.Usuario;
import com.contrastiq.backend.model.enums.NombreRol;
import com.contrastiq.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

// Resuelve "quien soy y de que sede" a partir del JWT de la peticion
// actual. Creado para cerrar el hallazgo DEF-03 del QA de julio 2026:
// ningun servicio filtraba resultados por la sede del usuario
// autenticado, pese a que `usuarios.sede_id` existe en el esquema desde
// el inicio -- cualquier usuario podia consultar por ID directo datos
// clinicos/operativos de una sede distinta a la suya.
//
// Sigue el mismo patron ya usado en PermisoAspect (JOIN FETCH explicito
// en vez de depender de una sesion de Hibernate abierta, para evitar
// LazyInitializationException al leer usuario.getRol()/usuario.getSede()
// fuera de un metodo @Transactional).
@Component
@RequiredArgsConstructor
public class UsuarioAutenticadoService {

    private final UsuarioRepository usuarioRepository;

    public Usuario obtenerUsuarioActual() {
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                : null;

        if (!(principal instanceof Jwt jwt)) {
            throw new AccessDeniedException("No autenticado");
        }

        return usuarioRepository.findByEmailConRolYSede(jwt.getSubject())
                .orElseThrow(() -> new AccessDeniedException("No autenticado"));
    }

    // Devuelve el id de la sede a la que el usuario actual debe quedar
    // restringido, o null si NO debe restringirse (dos casos legitimos):
    //   1. Rol ADMIN -- por diseno, siempre ve todas las sedes.
    //   2. Usuario sin sede asignada (usuarios.sede_id NULL) -- se
    //      interpreta como un usuario corporativo/ejecutivo sin sede fija,
    //      no como un error de datos (la columna es nullable a proposito
    //      en el esquema original).
    //
    // Los servicios que llaman a esto deben, cuando el resultado no es
    // null: (a) forzar ese valor como filtro en vez de confiar en lo que
    // mande el frontend, y (b) rechazar con AccessDeniedException
    // cualquier lectura de un recurso que pertenezca a otra sede.
    public Long sedeIdRestriccion() {
        Usuario usuario = obtenerUsuarioActual();
        if (usuario.getRol().getNombre() == NombreRol.ADMIN) {
            return null;
        }
        return usuario.getSede() != null ? usuario.getSede().getId() : null;
    }
}
