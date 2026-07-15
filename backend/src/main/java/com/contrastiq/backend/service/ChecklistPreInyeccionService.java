package com.contrastiq.backend.service;

import com.contrastiq.backend.dto.ChecklistPreInyeccionDTO;
import com.contrastiq.backend.dto.CrearChecklistRequest;
import com.contrastiq.backend.model.ChecklistPreInyeccion;
import com.contrastiq.backend.model.Paciente;
import com.contrastiq.backend.model.Sala;
import com.contrastiq.backend.model.Usuario;
import com.contrastiq.backend.repository.ChecklistPreInyeccionRepository;
import com.contrastiq.backend.repository.PacienteRepository;
import com.contrastiq.backend.repository.SalaRepository;
import com.contrastiq.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

// "Seguridad del paciente": checklist obligatorio antes de inyectar.
// Cada check de seguridad (identidad, GFR, alergias, consentimiento) se
// valida como @AssertTrue en el request -- no se puede guardar un
// checklist a medias, por diseno.
@Service
@RequiredArgsConstructor
public class ChecklistPreInyeccionService {

    private static final BigDecimal UMBRAL_GFR_RIESGO = BigDecimal.valueOf(60);

    private final ChecklistPreInyeccionRepository checklistRepository;
    private final PacienteRepository pacienteRepository;
    private final SalaRepository salaRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public List<ChecklistPreInyeccionDTO> historialPorPaciente(Long pacienteId) {
        return checklistRepository.findByPaciente_IdOrderByFechaHoraDesc(pacienteId).stream()
                .map(this::aDto)
                .toList();
    }

    @Transactional
    public ChecklistPreInyeccionDTO crear(CrearChecklistRequest request, Authentication authentication) {
        Paciente paciente = pacienteRepository.findById(request.getPacienteId())
                .orElseThrow(() -> new IllegalArgumentException("El paciente no existe"));

        Sala sala = request.getSalaId() != null
                ? salaRepository.findById(request.getSalaId())
                        .orElseThrow(() -> new IllegalArgumentException("La sala no existe"))
                : null;

        Usuario operador = resolverUsuarioAutenticado(authentication);

        boolean riesgoRenal = paciente.getGfrMlMin() != null
                && paciente.getGfrMlMin().compareTo(UMBRAL_GFR_RIESGO) < 0;

        ChecklistPreInyeccion checklist = ChecklistPreInyeccion.builder()
                .paciente(paciente)
                .sala(sala)
                .operador(operador)
                .identidadVerificada(request.getIdentidadVerificada())
                .gfrRevisado(request.getGfrRevisado())
                .gfrValorMomento(paciente.getGfrMlMin())
                .riesgoRenalMomento(riesgoRenal)
                .alergiasRevisadas(request.getAlergiasRevisadas())
                .alergiasMomento(paciente.getAlergias())
                .consentimientoFirmado(request.getConsentimientoFirmado())
                .observaciones(request.getObservaciones())
                .firmaNombre(request.getFirmaNombre())
                .firmaImagenBase64(request.getFirmaImagenBase64())
                .build();

        return aDto(checklistRepository.save(checklist));
    }

    // El JWT propio (unico metodo: login local) siempre usa el email
    // como "sub", tanto para login local como con Google.
    private Usuario resolverUsuarioAutenticado(Authentication authentication) {
        String email = ((Jwt) authentication.getPrincipal()).getSubject();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado"));
    }

    private ChecklistPreInyeccionDTO aDto(ChecklistPreInyeccion c) {
        return ChecklistPreInyeccionDTO.builder()
                .id(c.getId())
                .pacienteId(c.getPaciente().getId())
                .sala(c.getSala() != null ? c.getSala().getNombre() : null)
                .operador(c.getOperador().getNombreCompleto())
                .identidadVerificada(c.getIdentidadVerificada())
                .gfrRevisado(c.getGfrRevisado())
                .gfrValorMomento(c.getGfrValorMomento())
                .riesgoRenalMomento(c.getRiesgoRenalMomento())
                .alergiasRevisadas(c.getAlergiasRevisadas())
                .alergiasMomento(c.getAlergiasMomento())
                .consentimientoFirmado(c.getConsentimientoFirmado())
                .observaciones(c.getObservaciones())
                .firmaNombre(c.getFirmaNombre())
                .firmaImagenBase64(c.getFirmaImagenBase64())
                .fechaHora(c.getFechaHora())
                .build();
    }
}
