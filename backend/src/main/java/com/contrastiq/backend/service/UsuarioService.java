package com.contrastiq.backend.service;

import com.contrastiq.backend.dto.ActualizarUsuarioRequest;
import com.contrastiq.backend.dto.CrearUsuarioRequest;
import com.contrastiq.backend.dto.UsuarioResumenDTO;
import com.contrastiq.backend.model.Sede;
import com.contrastiq.backend.model.Usuario;
import com.contrastiq.backend.model.enums.ProveedorAutenticacion;
import com.contrastiq.backend.repository.HistorialAccesoRepository;
import com.contrastiq.backend.repository.RolRepository;
import com.contrastiq.backend.repository.SedeRepository;
import com.contrastiq.backend.repository.TokenRefrescoRepository;
import com.contrastiq.backend.repository.UsuarioRepository;
import com.contrastiq.backend.repository.spec.UsuarioSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// Pantalla "Gestion de usuarios": alta, edicion, activar/desactivar.
// Toda mutacion queda restringida a ADMIN a nivel de controller
// (@PreAuthorize), no solo aqui.
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final SedeRepository sedeRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenRefrescoRepository tokenRefrescoRepository;
    private final HistorialAccesoRepository historialAccesoRepository;

    @Transactional(readOnly = true)
    public Page<UsuarioResumenDTO> buscar(Long sedeId, Long rolId, Boolean activo, String busqueda, Pageable pageable) {
        return usuarioRepository
                .findAll(UsuarioSpecification.conFiltros(sedeId, rolId, activo, busqueda), pageable)
                .map(this::aResumen);
    }

    @Transactional(readOnly = true)
    public UsuarioResumenDTO obtener(Long id) {
        return aResumen(buscarPorId(id));
    }

    @Transactional
    public UsuarioResumenDTO crear(CrearUsuarioRequest request) {
        usuarioRepository.findByEmail(request.getEmail()).ifPresent(u -> {
            throw new IllegalArgumentException("Ya existe una cuenta con ese correo");
        });

        Usuario usuario = Usuario.builder()
                .nombreCompleto(request.getNombreCompleto())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .proveedor(ProveedorAutenticacion.LOCAL)
                .emailVerificado(false)
                .rol(rolRepository.findById(request.getRolId())
                        .orElseThrow(() -> new IllegalArgumentException("El rol seleccionado no existe")))
                .sede(resolverSede(request.getSedeId()))
                .activo(true)
                .build();

        return aResumen(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResumenDTO actualizar(Long id, ActualizarUsuarioRequest request) {
        Usuario usuario = buscarPorId(id);
        usuario.setNombreCompleto(request.getNombreCompleto());
        usuario.setRol(rolRepository.findById(request.getRolId())
                .orElseThrow(() -> new IllegalArgumentException("El rol seleccionado no existe")));
        usuario.setSede(resolverSede(request.getSedeId()));
        return aResumen(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResumenDTO cambiarEstado(Long id, boolean activo) {
        Usuario usuario = buscarPorId(id);
        usuario.setActivo(activo);
        return aResumen(usuarioRepository.save(usuario));
    }

    private Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El usuario no existe"));
    }

    private Sede resolverSede(Long sedeId) {
        if (sedeId == null) return null;
        return sedeRepository.findById(sedeId)
                .orElseThrow(() -> new IllegalArgumentException("La sede seleccionada no existe"));
    }

    private UsuarioResumenDTO aResumen(Usuario u) {
        // N+1 aceptado deliberadamente (2 consultas extra por fila de
        // usuario): a la escala real de ContrastIQ (decenas de usuarios,
        // no miles) el costo es insignificante frente a la simplicidad de
        // no mantener un cache/proyeccion aparte. Ver notas del documento
        // de referencia CEROGAS GPS.
        boolean online = tokenRefrescoRepository.existsByUsuario_IdAndRevocadoFalseAndExpiraEnAfter(
                u.getId(), LocalDateTime.now());
        LocalDateTime ultimoLogin = historialAccesoRepository
                .findTopByUsuario_IdAndExitosoTrueOrderByFechaHoraDesc(u.getId())
                .map(h -> h.getFechaHora())
                .orElse(null);

        return UsuarioResumenDTO.builder()
                .id(u.getId())
                .nombreCompleto(u.getNombreCompleto())
                .email(u.getEmail())
                .rol(u.getRol().getNombre().name())
                .sede(u.getSede() != null ? u.getSede().getNombre() : null)
                .sedeId(u.getSede() != null ? u.getSede().getId() : null)
                .activo(u.getActivo())
                .proveedor(u.getProveedor().name())
                .online(online)
                .ultimoLogin(ultimoLogin)
                .build();
    }
}
