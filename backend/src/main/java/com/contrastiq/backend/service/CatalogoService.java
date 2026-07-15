package com.contrastiq.backend.service;

import com.contrastiq.backend.dto.OpcionFiltroDTO;
import com.contrastiq.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

// Alimenta los <select> de la barra de filtros del dashboard: sedes,
// salas, inyectores, protocolos, agentes de contraste e identificadores
// anatomicos. Cada metodo devuelve pares {id, etiqueta} listos para Angular.
@Service
@RequiredArgsConstructor
public class CatalogoService {

    private final SedeRepository sedeRepository;
    private final SalaRepository salaRepository;
    private final InyectorRepository inyectorRepository;
    private final ProtocoloInyeccionRepository protocoloRepository;
    private final AgenteContrasteRepository agenteRepository;
    private final IdentificadorAnatomicoRepository identificadorRepository;
    private final RolRepository rolRepository;

    public List<OpcionFiltroDTO> roles() {
        return rolRepository.findAll().stream()
                .map(r -> OpcionFiltroDTO.builder().id(r.getId()).etiqueta(r.getNombre().name()).build())
                .toList();
    }

    public List<OpcionFiltroDTO> sedes() {
        return sedeRepository.findAll().stream()
                .map(s -> OpcionFiltroDTO.builder().id(s.getId()).etiqueta(s.getNombre()).build())
                .toList();
    }

    public List<OpcionFiltroDTO> salasPorSede(Long sedeId) {
        List<com.contrastiq.backend.model.Sala> salas = (sedeId == null)
                ? salaRepository.findAll()
                : salaRepository.findBySedeIdAndActivoTrue(sedeId);
        return salas.stream()
                .map(s -> OpcionFiltroDTO.builder().id(s.getId()).etiqueta(s.getNombre()).build())
                .toList();
    }

    public List<OpcionFiltroDTO> inyectoresPorSala(Long salaId) {
        List<com.contrastiq.backend.model.Inyector> inyectores = (salaId == null)
                ? inyectorRepository.findAll()
                : inyectorRepository.findBySalaId(salaId);
        return inyectores.stream()
                .map(i -> OpcionFiltroDTO.builder()
                        .id(i.getId())
                        .etiqueta(i.getSala().getNombre() + " — " + i.getNumeroSerie())
                        .build())
                .toList();
    }

    public List<OpcionFiltroDTO> protocolos(Long identificadorAnatomicoId) {
        List<com.contrastiq.backend.model.ProtocoloInyeccion> protocolos = (identificadorAnatomicoId == null)
                ? protocoloRepository.findByActivoTrue()
                : protocoloRepository.findByIdentificadorAnatomicoIdAndActivoTrue(identificadorAnatomicoId);
        return protocolos.stream()
                .map(p -> OpcionFiltroDTO.builder().id(p.getId()).etiqueta(p.getNombre()).build())
                .toList();
    }

    public List<OpcionFiltroDTO> agentesContraste() {
        return agenteRepository.findByActivoTrue().stream()
                .map(a -> OpcionFiltroDTO.builder().id(a.getId()).etiqueta(a.getNombreComercial()).build())
                .toList();
    }

    public List<OpcionFiltroDTO> identificadoresAnatomicos() {
        return identificadorRepository.findAll().stream()
                .map(ia -> OpcionFiltroDTO.builder().id(ia.getId()).etiqueta(ia.getNombre()).build())
                .toList();
    }
}
