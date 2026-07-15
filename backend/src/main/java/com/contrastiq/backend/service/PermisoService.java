package com.contrastiq.backend.service;

import com.contrastiq.backend.dto.MatrizCeldaDTO;
import com.contrastiq.backend.dto.ModuloDTO;
import com.contrastiq.backend.dto.PermisoDTO;
import com.contrastiq.backend.dto.PermisoModuloDTO;
import com.contrastiq.backend.dto.RolDTO;
import com.contrastiq.backend.model.Modulo;
import com.contrastiq.backend.model.Permiso;
import com.contrastiq.backend.model.Rol;
import com.contrastiq.backend.model.RolModuloPermiso;
import com.contrastiq.backend.model.RolModuloPermisoId;
import com.contrastiq.backend.model.enums.NombreRol;
import com.contrastiq.backend.repository.ModuloRepository;
import com.contrastiq.backend.repository.PermisoRepository;
import com.contrastiq.backend.repository.RolModuloPermisoRepository;
import com.contrastiq.backend.repository.RolRepository;
import com.contrastiq.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Pantalla "Roles y permisos" del modulo de administracion: consulta y
// edicion (checkbox por checkbox, sin boton "Guardar") de la matriz
// Rol x Modulo x Permiso.
//
// Nota sobre @Transactional: a diferencia de PermisoAspect (ver esa
// clase para la advertencia completa sobre @Around de AspectJ), este
// servicio SI es invocado a traves del proxy dinamico normal de Spring
// (llamado desde AdministracionController, un @RestController comun),
// asi que @Transactional aqui funciona sin problema para las relaciones
// LAZY que se tocan al armar los DTOs.
@Service
@RequiredArgsConstructor
public class PermisoService {

    private final ModuloRepository moduloRepository;
    private final PermisoRepository permisoRepository;
    private final RolRepository rolRepository;
    private final RolModuloPermisoRepository rolModuloPermisoRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public List<ModuloDTO> listarModulos() {
        return moduloRepository.findAllByOrderByOrdenAsc().stream()
                .map(m -> new ModuloDTO(m.getId(), m.getCodigo(), m.getNombre(), m.getDescripcion()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PermisoDTO> listarPermisos() {
        return permisoRepository.findAllByOrderByIdAsc().stream()
                .map(p -> new PermisoDTO(p.getId(), p.getCodigo(), p.getNombre(), p.getDescripcion()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RolDTO> listarRoles() {
        return rolRepository.findAll().stream()
                .map(r -> new RolDTO(r.getId(), r.getNombre().name(), usuarioRepository.count(
                        (root, query, cb) -> cb.equal(root.get("rol"), r))))
                .toList();
    }

    // Devuelve la matriz completa (todos los modulos x todos los
    // permisos) marcando cuales estan otorgados al rol solicitado.
    @Transactional(readOnly = true)
    public List<MatrizCeldaDTO> obtenerMatrizDeRol(Long rolId) {
        Rol rol = rolRepository.findById(rolId)
                .orElseThrow(() -> new IllegalArgumentException("El rol no existe"));

        List<Modulo> modulos = moduloRepository.findAllByOrderByOrdenAsc();
        List<Permiso> permisos = permisoRepository.findAllByOrderByIdAsc();
        List<RolModuloPermiso> otorgados = rolModuloPermisoRepository.findByRol_Id(rolId);

        return modulos.stream()
                .flatMap(m -> permisos.stream().map(p -> {
                    boolean otorgado = otorgados.stream().anyMatch(rmp ->
                            rmp.getModulo().getId().equals(m.getId())
                                    && rmp.getPermiso().getId().equals(p.getId()));
                    return new MatrizCeldaDTO(m.getId(), m.getCodigo(), m.getNombre(),
                            p.getId(), p.getCodigo(), p.getNombre(), otorgado);
                }))
                .toList();
    }

    @Transactional
    public void otorgar(Long rolId, Long moduloId, Long permisoId) {
        if (rolModuloPermisoRepository.existsByRol_IdAndModulo_IdAndPermiso_Id(rolId, moduloId, permisoId)) {
            return; // idempotente
        }
        Rol rol = rolRepository.findById(rolId)
                .orElseThrow(() -> new IllegalArgumentException("El rol no existe"));
        Modulo modulo = moduloRepository.findById(moduloId)
                .orElseThrow(() -> new IllegalArgumentException("El modulo no existe"));
        Permiso permiso = permisoRepository.findById(permisoId)
                .orElseThrow(() -> new IllegalArgumentException("El permiso no existe"));

        RolModuloPermiso rmp = RolModuloPermiso.builder()
                .id(new RolModuloPermisoId(rolId, moduloId, permisoId))
                .rol(rol)
                .modulo(modulo)
                .permiso(permiso)
                .build();
        rolModuloPermisoRepository.save(rmp);
    }

    @Transactional
    public void revocar(Long rolId, Long moduloId, Long permisoId) {
        // Regla de negocio: el rol ADMIN siempre debe conservar acceso
        // total -- si se le pudiera revocar el ultimo permiso, un admin
        // podria bloquearse a si mismo (y a cualquier otro admin) fuera
        // del propio modulo de administracion sin forma de revertirlo
        // desde la UI.
        Rol rol = rolRepository.findById(rolId)
                .orElseThrow(() -> new IllegalArgumentException("El rol no existe"));
        if (rol.getNombre() == NombreRol.ADMIN) {
            throw new IllegalArgumentException("No se le pueden revocar permisos al rol ADMIN");
        }
        rolModuloPermisoRepository.deleteByRol_IdAndModulo_IdAndPermiso_Id(rolId, moduloId, permisoId);
    }

    // Consumido por /api/me/permisos -- cualquier usuario autenticado ve
    // sus propios permisos (no requiere ser ADMIN).
    @Transactional(readOnly = true)
    public List<PermisoModuloDTO> permisosDelRol(NombreRol nombreRol) {
        return rolModuloPermisoRepository.obtenerPermisosDeRol(nombreRol);
    }
}
