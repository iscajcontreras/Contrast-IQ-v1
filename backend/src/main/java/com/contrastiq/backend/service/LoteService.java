package com.contrastiq.backend.service;

import com.contrastiq.backend.dto.CrearLoteRequest;
import com.contrastiq.backend.dto.LoteDTO;
import com.contrastiq.backend.dto.TrazabilidadLoteDTO;
import com.contrastiq.backend.model.AgenteContraste;
import com.contrastiq.backend.model.InyeccionFase;
import com.contrastiq.backend.model.LoteAgenteContraste;
import com.contrastiq.backend.model.Sede;
import com.contrastiq.backend.repository.AgenteContrasteRepository;
import com.contrastiq.backend.repository.InyeccionFaseRepository;
import com.contrastiq.backend.repository.LoteAgenteContrasteRepository;
import com.contrastiq.backend.repository.SedeRepository;
import com.contrastiq.backend.repository.spec.LoteAgenteContrasteSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

// Trazabilidad de insumos: registrar lotes de agente de contraste
// (numero de lote, caducidad, cantidad recibida) y, ante un recall del
// fabricante, responder de inmediato "que pacientes recibieron este lote".
@Service
@RequiredArgsConstructor
public class LoteService {

    private final LoteAgenteContrasteRepository loteRepository;
    private final InyeccionFaseRepository inyeccionFaseRepository;
    private final AgenteContrasteRepository agenteRepository;
    private final SedeRepository sedeRepository;

    @Transactional(readOnly = true)
    public Page<LoteDTO> buscar(Long sedeId, Long agenteId, Boolean soloVigentes, Boolean proximosACaducar, Pageable pageable) {
        return loteRepository
                .findAll(LoteAgenteContrasteSpecification.conFiltros(sedeId, agenteId, soloVigentes, proximosACaducar), pageable)
                .map(this::aDto);
    }

    @Transactional
    public LoteDTO crear(CrearLoteRequest request) {
        AgenteContraste agente = agenteRepository.findById(request.getAgenteId())
                .orElseThrow(() -> new IllegalArgumentException("El agente de contraste no existe"));
        Sede sede = sedeRepository.findById(request.getSedeId())
                .orElseThrow(() -> new IllegalArgumentException("La sede no existe"));

        loteRepository.findByNumeroLoteAndAgenteIdAndSedeId(request.getNumeroLote(), agente.getId(), sede.getId())
                .ifPresent(l -> {
                    throw new IllegalArgumentException("Ese lote ya esta registrado para este agente y sede");
                });

        LoteAgenteContraste lote = LoteAgenteContraste.builder()
                .agente(agente)
                .sede(sede)
                .numeroLote(request.getNumeroLote())
                .fechaCaducidad(request.getFechaCaducidad())
                .cantidadMl(request.getCantidadMl())
                .activo(true)
                .build();

        return aDto(loteRepository.save(lote));
    }

    // El corazon de la funcionalidad: dado un lote, quienes lo recibieron.
    @Transactional(readOnly = true)
    public List<TrazabilidadLoteDTO> trazabilidad(Long loteId) {
        List<InyeccionFase> fases = inyeccionFaseRepository.findByLoteAgenteIdOrderByInyeccion_FechaHoraInicioDesc(loteId);

        return fases.stream()
                .map(f -> {
                    var i = f.getInyeccion();
                    return TrazabilidadLoteDTO.builder()
                            .inyeccionId(i.getId())
                            .fechaHoraInyeccion(i.getFechaHoraInicio())
                            .pacienteIdentificador(i.getPaciente() != null ? i.getPaciente().getIdentificadorExterno() : "—")
                            .pacienteNombre(i.getPaciente() != null ? i.getPaciente().getNombreCompleto() : null)
                            .sede(i.getInyector().getSala().getSede().getNombre())
                            .sala(i.getInyector().getSala().getNombre())
                            .inyector(i.getInyector().getNumeroSerie())
                            .protocolo(i.getProtocolo() != null ? i.getProtocolo().getNombre() : "—")
                            .operador(i.getOperador() != null ? i.getOperador().getNombreCompleto() : "—")
                            .estadoInyeccion(i.getEstado().name())
                            .build();
                })
                .toList();
    }

    private LoteDTO aDto(LoteAgenteContraste l) {
        long diasParaCaducar = ChronoUnit.DAYS.between(LocalDate.now(), l.getFechaCaducidad());
        return LoteDTO.builder()
                .id(l.getId())
                .numeroLote(l.getNumeroLote())
                .agente(l.getAgente().getNombreComercial())
                .sede(l.getSede().getNombre())
                .fechaCaducidad(l.getFechaCaducidad())
                .cantidadMl(l.getCantidadMl())
                .recibidoEn(l.getRecibidoEn())
                .activo(l.getActivo())
                .vencido(diasParaCaducar < 0)
                .diasParaCaducar(diasParaCaducar)
                .build();
    }
}
