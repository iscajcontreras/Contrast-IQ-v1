package com.contrastiq.backend.service;

import com.contrastiq.backend.model.Auditoria;
import com.contrastiq.backend.model.Usuario;
import com.contrastiq.backend.repository.AuditoriaRepository;
import com.contrastiq.backend.security.UsuarioAutenticadoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

// Fix DEF-02 (QA julio 2026): la tabla `auditoria` existia desde el
// esquema original pero no tenia ni un solo repositorio/servicio que la
// usara -- ninguna accion administrativa (crear usuario, otorgar/revocar
// permiso, crear lote) quedaba registrada pese a que el modulo
// "Historial de accesos" hacia pensar que si habia trazabilidad de
// acciones administrativas.
//
// Cobertura de este pase (deliberadamente NO exhaustiva -- ver nota en
// PROXIMOS_PASOS.md): UsuarioService (crear/actualizar/cambiarEstado),
// PermisoService (otorgar/revocar), LoteService (crear). Extender a
// el resto de mutaciones del sistema es un trabajo aparte.
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioAutenticadoService usuarioAutenticadoService;
    private final ObjectMapper objectMapper;

    // Deliberadamente "best effort": un fallo al registrar la auditoria
    // (ej. no se pudo serializar el detalle) nunca debe tumbar la accion
    // de negocio real que la origino -- por eso todo el metodo esta
    // envuelto en un try/catch que solo loguea.
    @Transactional
    public void registrar(String tablaAfectada, Long registroId, Auditoria.Accion accion, Map<String, Object> detalle) {
        try {
            String detalleJson = null;
            if (detalle != null && !detalle.isEmpty()) {
                detalleJson = objectMapper.writeValueAsString(detalle);
            }

            Usuario usuario = null;
            try {
                usuario = usuarioAutenticadoService.obtenerUsuarioActual();
            } catch (Exception e) {
                // Sin usuario autenticado resoluble (ej. un proceso batch
                // futuro) -- se deja usuario_id NULL, la columna es
                // nullable a proposito en el esquema original.
            }

            Auditoria registro = Auditoria.builder()
                    .tablaAfectada(tablaAfectada)
                    .registroId(registroId)
                    .usuario(usuario)
                    .accion(accion)
                    .detalleJson(detalleJson)
                    .build();
            auditoriaRepository.save(registro);
        } catch (Exception e) {
            log.error("No se pudo registrar auditoria para {} #{} ({}): {}",
                    tablaAfectada, registroId, accion, e.getMessage(), e);
        }
    }
}
