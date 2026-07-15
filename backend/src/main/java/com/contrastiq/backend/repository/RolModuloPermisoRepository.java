package com.contrastiq.backend.repository;

import com.contrastiq.backend.dto.PermisoModuloDTO;
import com.contrastiq.backend.model.RolModuloPermiso;
import com.contrastiq.backend.model.RolModuloPermisoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RolModuloPermisoRepository extends JpaRepository<RolModuloPermiso, RolModuloPermisoId> {

    // Usada por PermisoAspect para validar @RequierePermiso. Va por
    // codigo de rol/modulo/permiso (no por id) porque el aspecto solo
    // tiene el nombre del rol disponible desde el JWT/usuario autenticado.
    @Query("""
            select case when count(rmp) > 0 then true else false end
            from RolModuloPermiso rmp
            where rmp.rol.nombre = :nombreRol
              and rmp.modulo.codigo = :moduloCodigo
              and rmp.permiso.codigo = :permisoCodigo
            """)
    boolean tienePermiso(@Param("nombreRol") com.contrastiq.backend.model.enums.NombreRol nombreRol,
                          @Param("moduloCodigo") String moduloCodigo,
                          @Param("permisoCodigo") String permisoCodigo);

    // Todos los permisos concedidos a un rol (por nombre de rol), para
    // /api/me/permisos. Constructor expression explicito -- no proyeccion
    // de interfaz -- para evitar el problema de proxies dinamicos de
    // Jackson documentado en el proyecto de referencia.
    @Query("""
            select new com.contrastiq.backend.dto.PermisoModuloDTO(rmp.modulo.codigo, rmp.permiso.codigo)
            from RolModuloPermiso rmp
            where rmp.rol.nombre = :nombreRol
            """)
    List<PermisoModuloDTO> obtenerPermisosDeRol(@Param("nombreRol") com.contrastiq.backend.model.enums.NombreRol nombreRol);

    List<RolModuloPermiso> findByRol_Id(Long rolId);

    void deleteByRol_IdAndModulo_IdAndPermiso_Id(Long rolId, Long moduloId, Long permisoId);

    boolean existsByRol_IdAndModulo_IdAndPermiso_Id(Long rolId, Long moduloId, Long permisoId);
}
