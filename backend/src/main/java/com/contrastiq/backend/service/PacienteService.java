package com.contrastiq.backend.service;

import com.contrastiq.backend.dto.HistorialInyeccionPacienteDTO;
import com.contrastiq.backend.dto.PacienteDetalleDTO;
import com.contrastiq.backend.dto.PacienteResumenDTO;
import com.contrastiq.backend.dto.ReaccionPacienteDTO;
import com.contrastiq.backend.model.EventoExtravasacion;
import com.contrastiq.backend.model.Inyeccion;
import com.contrastiq.backend.model.Paciente;
import com.contrastiq.backend.model.enums.EstadoEda;
import com.contrastiq.backend.model.enums.EstadoInyeccion;
import com.contrastiq.backend.repository.EventoExtravasacionRepository;
import com.contrastiq.backend.repository.InyeccionRepository;
import com.contrastiq.backend.repository.PacienteRepository;
import com.contrastiq.backend.repository.spec.PacienteSpecification;
import com.contrastiq.backend.security.UsuarioAutenticadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

// Dashboard de Paciente: vista centrada en la persona, no en el equipo.
// Util, por ejemplo, para que un radiologo revise el historial de
// contraste de un paciente (reacciones previas, funcion renal) antes de
// autorizar una nueva inyeccion.
@Service
@RequiredArgsConstructor
public class PacienteService {

    private final PacienteRepository pacienteRepository;
    private final InyeccionRepository inyeccionRepository;
    private final EventoExtravasacionRepository eventoExtravasacionRepository;
    private final UsuarioAutenticadoService usuarioAutenticadoService;

    // Umbral clinico habitual para precaucion con contraste yodado
    // (nefropatia inducida por contraste). Esto es una simplificacion
    // de referencia, NO sustituye el criterio clinico del radiologo.
    private static final BigDecimal UMBRAL_GFR_RIESGO = BigDecimal.valueOf(60);

    @Transactional(readOnly = true)
    public Page<PacienteResumenDTO> buscar(String busqueda, Pageable pageable) {
        // Fix DEF-03 (QA julio 2026): un usuario restringido a una sede
        // solo debe ver, en la busqueda, pacientes con al menos una
        // inyeccion registrada en su propia sede.
        Long restriccion = usuarioAutenticadoService.sedeIdRestriccion();
        Specification<Paciente> spec = PacienteSpecification.conBusqueda(busqueda)
                .and(PacienteSpecification.conSedeRestriccion(restriccion));
        return pacienteRepository.findAll(spec, pageable)
                .map(this::aResumen);
    }

    // Fix DEF-03 (QA julio 2026): valida que el usuario actual tenga
    // acceso a este paciente (sin restriccion, o con al menos una
    // inyeccion del paciente en su propia sede) antes de exponer
    // cualquier dato clinico suyo. Se llama al inicio de los 3 metodos
    // de detalle -- si esto no se hiciera, un usuario podia saltarse el
    // filtro de conSedeRestriccion() de buscar() con solo conocer el ID.
    private void verificarAccesoAPaciente(Long pacienteId) {
        Long restriccion = usuarioAutenticadoService.sedeIdRestriccion();
        if (restriccion == null) {
            return;
        }
        boolean tieneAcceso = inyeccionRepository.existsByPaciente_IdAndInyector_Sala_Sede_Id(pacienteId, restriccion);
        if (!tieneAcceso) {
            throw new AccessDeniedException("No tienes acceso a la informacion de este paciente");
        }
    }

    @Transactional(readOnly = true)
    public PacienteDetalleDTO obtenerDetalle(Long id) {
        verificarAccesoAPaciente(id);
        Paciente paciente = pacienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El paciente no existe"));

        long total = inyeccionRepository.countByPaciente_Id(id);
        BigDecimal volumenTotal = inyeccionRepository.sumarVolumenPorPaciente(id);
        long abortadasOError = inyeccionRepository.countByPaciente_IdAndEstadoIn(
                id, List.of(EstadoInyeccion.ABORTADA, EstadoInyeccion.ERROR));
        long edaFueraDeRango = inyeccionRepository.contarEdaFueraDeRangoPorPaciente(id);

        boolean riesgoRenal = paciente.getGfrMlMin() != null
                && paciente.getGfrMlMin().compareTo(UMBRAL_GFR_RIESGO) < 0;

        return PacienteDetalleDTO.builder()
                .id(paciente.getId())
                .identificadorExterno(paciente.getIdentificadorExterno())
                .nombreCompleto(paciente.getNombreCompleto())
                .sexo(paciente.getSexo().name())
                .pesoKg(paciente.getPesoKg())
                .creatininaMgDl(paciente.getCreatininaMgDl())
                .gfrMlMin(paciente.getGfrMlMin())
                .riesgoRenal(riesgoRenal)
                .totalInyecciones(total)
                .volumenTotalRecibidoMl(volumenTotal)
                .dlpTotalMgyCm(inyeccionRepository.sumarDlpPorPaciente(id))
                .ultimaInyeccion(inyeccionRepository.ultimaInyeccionPorPaciente(id))
                .alertasEdaFueraDeRango(edaFueraDeRango)
                .inyeccionesAbortadasOError(abortadasOError)
                .build();
    }

    @Transactional(readOnly = true)
    public List<HistorialInyeccionPacienteDTO> historialInyecciones(Long id) {
        verificarAccesoAPaciente(id);
        List<Inyeccion> inyecciones = inyeccionRepository.findByPaciente_IdOrderByFechaHoraInicioDesc(id);

        return inyecciones.stream().map(i -> {
            boolean tieneAlerta = i.getEventosExtravasacion().stream()
                    .anyMatch(e -> e.getEstadoEda() == EstadoEda.FUERA_DE_RANGO);

            String agentePrincipal = i.getFases().stream()
                    .filter(f -> Boolean.TRUE.equals(f.getEsFaseContraste()))
                    .findFirst()
                    .map(f -> f.getAgente().getNombreComercial())
                    .orElse("—");

            return HistorialInyeccionPacienteDTO.builder()
                    .inyeccionId(i.getId())
                    .fechaHoraInicio(i.getFechaHoraInicio())
                    .sede(i.getInyector().getSala().getSede().getNombre())
                    .sala(i.getInyector().getSala().getNombre())
                    .protocolo(i.getProtocolo() != null ? i.getProtocolo().getNombre() : "—")
                    .identificadorAnatomico(i.getIdentificadorAnatomico() != null
                            ? i.getIdentificadorAnatomico().getNombre() : "—")
                    .agentePrincipal(agentePrincipal)
                    .volumenTotalMl(i.getVolumenTotalMl())
                    .dlpMgyCm(i.getDlpMgyCm())
                    .presionMaximaPsi(i.getPresionMaximaPsi())
                    .edaHabilitado(i.getEdaHabilitado())
                    .tieneSeriePresion(!i.getSeriePresion().isEmpty())
                    .estado(i.getEstado().name())
                    .tieneAlertaEda(tieneAlerta)
                    .operador(i.getOperador() != null ? i.getOperador().getNombreCompleto() : "—")
                    .build();
        }).toList();
    }

    // "Historial de reacciones por paciente" (Seguridad del paciente)
    @Transactional(readOnly = true)
    public List<ReaccionPacienteDTO> reacciones(Long pacienteId) {
        verificarAccesoAPaciente(pacienteId);
        return eventoExtravasacionRepository.findByInyeccion_Paciente_IdOrderByFechaHoraDesc(pacienteId).stream()
                .map(this::aReaccionDto)
                .toList();
    }

    private ReaccionPacienteDTO aReaccionDto(EventoExtravasacion e) {
        Inyeccion i = e.getInyeccion();
        String agentePrincipal = i.getFases().stream()
                .filter(f -> Boolean.TRUE.equals(f.getEsFaseContraste()))
                .findFirst()
                .map(f -> f.getAgente().getNombreComercial())
                .orElse("—");

        return ReaccionPacienteDTO.builder()
                .eventoId(e.getId())
                .inyeccionId(i.getId())
                .fechaHora(e.getFechaHora())
                .estadoEda(e.getEstadoEda().name())
                .revisado(e.getRevisado())
                .accionTomada(e.getAccionTomada())
                .notas(e.getNotas())
                .protocolo(i.getProtocolo() != null ? i.getProtocolo().getNombre() : "—")
                .agentePrincipal(agentePrincipal)
                .build();
    }

    private PacienteResumenDTO aResumen(Paciente p) {
        return PacienteResumenDTO.builder()
                .id(p.getId())
                .identificadorExterno(p.getIdentificadorExterno())
                .nombreCompleto(p.getNombreCompleto())
                .sexo(p.getSexo().name())
                .build();
    }
}
