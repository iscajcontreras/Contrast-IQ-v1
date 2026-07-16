package com.contrastiq.backend.service;

import com.contrastiq.backend.dto.HistorialAccesoDTO;
import com.contrastiq.backend.model.HistorialAcceso;
import com.contrastiq.backend.model.enums.ProveedorAutenticacion;
import com.contrastiq.backend.repository.HistorialAccesoRepository;
import com.contrastiq.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// "Historial de accesos visible en la UI": registra cada intento de
// login (exitoso o fallido) y lo expone para la pantalla de gestion de
// usuarios. Se llama directo desde AuthService.login() (antes se
// llamaba desde los handlers de formLogin, que ya no existen ahora que
// el login es una llamada REST directa).
@Service
@RequiredArgsConstructor
public class HistorialAccesoService {

    private final HistorialAccesoRepository historialRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public void registrar(String email, boolean exitoso, ProveedorAutenticacion metodo,
                           String ipOrigen, String userAgent) {
        HistorialAcceso registro = HistorialAcceso.builder()
                .usuario(usuarioRepository.findByEmail(email).orElse(null))
                .emailUsado(email)
                .exitoso(exitoso)
                .metodo(metodo)
                .ipOrigen(ipOrigen)
                .userAgent(userAgent)
                .build();
        historialRepository.save(registro);
    }

    // Fix DEF-01 (QA julio 2026): usado por AuthService.login() para
    // decidir si debe bloquear temporalmente la cuenta tras varios
    // intentos fallidos consecutivos dentro de una ventana de tiempo.
    @Transactional(readOnly = true)
    public long contarFallidosRecientes(String email, LocalDateTime desde) {
        return historialRepository.countByEmailUsadoAndExitosoFalseAndFechaHoraAfter(email, desde);
    }

    @Transactional(readOnly = true)
    public Page<HistorialAccesoDTO> listarPorUsuario(Long usuarioId, Pageable pageable) {
        return historialRepository.findByUsuario_IdOrderByFechaHoraDesc(usuarioId, pageable).map(this::aDto);
    }

    @Transactional(readOnly = true)
    public Page<HistorialAccesoDTO> listarTodos(Pageable pageable) {
        return historialRepository.findAllByOrderByFechaHoraDesc(pageable).map(this::aDto);
    }

    private HistorialAccesoDTO aDto(HistorialAcceso h) {
        return HistorialAccesoDTO.builder()
                .id(h.getId())
                .emailUsado(h.getEmailUsado())
                .exitoso(h.getExitoso())
                .metodo(h.getMetodo().name())
                .ipOrigen(h.getIpOrigen())
                .userAgent(h.getUserAgent())
                .fechaHora(h.getFechaHora())
                .build();
    }
}
