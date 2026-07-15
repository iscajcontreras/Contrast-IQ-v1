package com.contrastiq.backend.repository;

import com.contrastiq.backend.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {
    Optional<Usuario> findByEmail(String email);
    // findByProveedorAndProveedorId() se removio como limpieza de deuda
    // tecnica (Prioridad 4): solo servia para el lookup de login
    // federado con Google, que nunca se implemento y no tenia ningun
    // llamador real en el codigo.

    // JOIN FETCH explicito del rol: usado por PermisoAspect, que corre
    // via reflexion directa de AspectJ y NO respeta @Transactional (ver
    // notas en PermisoAspect) -- si se dejara el rol en LAZY normal,
    // acceder a usuario.getRol().getNombre() fuera de una sesion abierta
    // lanzaria LazyInitializationException. Con JOIN FETCH el rol ya
    // viene cargado en la misma consulta, sin depender de una sesion viva.
    @Query("select u from Usuario u join fetch u.rol where u.email = :email")
    Optional<Usuario> findByEmailConRol(@Param("email") String email);
}
