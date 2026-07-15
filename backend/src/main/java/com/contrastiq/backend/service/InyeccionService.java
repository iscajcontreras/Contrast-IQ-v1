package com.contrastiq.backend.service;

import com.contrastiq.backend.dto.ActualizarDosisRadiacionRequest;
import com.contrastiq.backend.dto.InyeccionDetalleCompletoResponse;
import com.contrastiq.backend.dto.InyeccionResumenDTO;
import com.contrastiq.backend.dto.PuntoPresionDTO;
import com.contrastiq.backend.dto.filtro.FiltroInyeccionDTO;
import com.contrastiq.backend.model.AgenteContraste;
import com.contrastiq.backend.model.Inyeccion;
import com.contrastiq.backend.model.InyeccionFase;
import com.contrastiq.backend.model.InyeccionFaseProgramada;
import com.contrastiq.backend.model.Paciente;
import com.contrastiq.backend.model.ProtocoloFase;
import com.contrastiq.backend.model.enums.EstadoEda;
import com.contrastiq.backend.repository.InyeccionFaseProgramadaRepository;
import com.contrastiq.backend.repository.InyeccionRepository;
import com.contrastiq.backend.repository.InyeccionSeriePresionRepository;
import com.contrastiq.backend.repository.InyeccionSerieFlujoRepository;
import com.contrastiq.backend.repository.ProtocoloFaseRepository;
import com.contrastiq.backend.repository.spec.InyeccionSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InyeccionService {

    private final InyeccionRepository inyeccionRepository;
    private final InyeccionSeriePresionRepository seriePresionRepository;
    private final InyeccionSerieFlujoRepository serieFlujoRepository;
    private final InyeccionFaseProgramadaRepository faseProgramadaRepository;
    private final ProtocoloFaseRepository protocoloFaseRepository;

    // Usado por GET /api/inyecciones : lista paginada que respeta TODOS los
    // filtros de la barra del dashboard (fecha, sala, inyector, protocolo,
    // identificador anatomico, agente, estado, solo-alertas-eda).
    @Transactional(readOnly = true)
    public Page<InyeccionResumenDTO> buscar(FiltroInyeccionDTO filtro, Pageable pageable) {
        Page<Inyeccion> pagina = inyeccionRepository.findAll(
                InyeccionSpecification.conFiltros(filtro), pageable);
        return pagina.map(this::aResumen);
    }

    // Usado por el boton "Ver presion" de la tabla: la grafica completa de
    // presion vs. tiempo de una inyeccion especifica (ver manual IRiS,
    // campo "Max. PSI").
    @Transactional(readOnly = true)
    public List<PuntoPresionDTO> obtenerSeriePresion(Long inyeccionId) {
        return seriePresionRepository.findByInyeccion_IdOrderByTiempoSegAsc(inyeccionId).stream()
                .map(p -> PuntoPresionDTO.builder()
                        .tiempoSeg(p.getTiempoSeg())
                        .presionPsi(p.getPresionPsi())
                        .build())
                .toList();
    }

    // "Dosis de radiacion combinada"
    @Transactional
    public InyeccionResumenDTO actualizarDosisRadiacion(Long inyeccionId, ActualizarDosisRadiacionRequest request) {
        Inyeccion i = inyeccionRepository.findById(inyeccionId)
                .orElseThrow(() -> new IllegalArgumentException("La inyeccion no existe"));
        i.setCtdiVolMgy(request.getCtdiVolMgy());
        i.setDlpMgyCm(request.getDlpMgyCm());
        return aResumen(inyeccionRepository.save(i));
    }

    // Usado por la pantalla de detalle completo de una inyeccion: junta
    // demograficos del paciente, agente/lote, metadatos clinicos, las
    // dos graficas (presion y flujo) y el comparativo de fases
    // Planeado / Programado / Real.
    @Transactional(readOnly = true)
    public InyeccionDetalleCompletoResponse obtenerDetalleCompleto(Long inyeccionId) {
        Inyeccion i = inyeccionRepository.findById(inyeccionId)
                .orElseThrow(() -> new IllegalArgumentException("La inyeccion no existe"));

        List<PuntoPresionDTO> seriePresion = seriePresionRepository
                .findByInyeccion_IdOrderByTiempoSegAsc(inyeccionId).stream()
                .map(p -> PuntoPresionDTO.builder()
                        .tiempoSeg(p.getTiempoSeg())
                        .presionPsi(p.getPresionPsi())
                        .build())
                .toList();

        List<InyeccionDetalleCompletoResponse.PuntoFlujoDTO> serieFlujo = serieFlujoRepository
                .findByInyeccion_IdOrderByTiempoSegAsc(inyeccionId).stream()
                .map(f -> InyeccionDetalleCompletoResponse.PuntoFlujoDTO.builder()
                        .tiempoSeg(f.getTiempoSeg())
                        .flujoContrasteMlS(f.getFlujoContrasteMlS())
                        .flujoSalinaMlS(f.getFlujoSalinaMlS())
                        .build())
                .toList();

        return InyeccionDetalleCompletoResponse.builder()
                .inyeccionId(i.getId())
                .paciente(aPacienteInfo(i.getPaciente()))
                .contraste(aContrasteInfo(i))
                .metadatos(aMetadatos(i))
                .seriePresion(seriePresion)
                .serieFlujo(serieFlujo)
                .comparativoFases(aComparativoFases(i))
                .build();
    }

    private InyeccionDetalleCompletoResponse.PacienteInfoDTO aPacienteInfo(Paciente p) {
        if (p == null) {
            return null;
        }
        return InyeccionDetalleCompletoResponse.PacienteInfoDTO.builder()
                .id(p.getId())
                .identificadorExterno(p.getIdentificadorExterno())
                .nombreCompleto(p.getNombreCompleto())
                .numeroExpediente(p.getNumeroExpediente())
                .sexo(p.getSexo() != null ? p.getSexo().name() : null)
                .fechaNacimiento(p.getFechaNacimiento())
                .pesoKg(p.getPesoKg())
                .tallaM(p.getTallaM())
                .grupoEtnico(p.getGrupoEtnico())
                .gfrMlMin(p.getGfrMlMin())
                .creatininaMgDl(p.getCreatininaMgDl())
                .alergias(p.getAlergias())
                .build();
    }

    private InyeccionDetalleCompletoResponse.ContrasteInfoDTO aContrasteInfo(Inyeccion i) {
        InyeccionFase faseContraste = i.getFases().stream()
                .filter(f -> Boolean.TRUE.equals(f.getEsFaseContraste()))
                .findFirst()
                .orElse(null);

        InyeccionDetalleCompletoResponse.ContrasteInfoDTO.ContrasteInfoDTOBuilder builder =
                InyeccionDetalleCompletoResponse.ContrasteInfoDTO.builder()
                        .dosisContrasteGl(i.getDosisContrasteGl())
                        .volumenTotalMl(i.getVolumenTotalMl());

        if (faseContraste != null) {
            AgenteContraste agente = faseContraste.getAgente();
            builder.agentePrincipal(agente != null ? agente.getNombreComercial() : null)
                    .concentracion(agente != null ? agente.getConcentracion() : null)
                    .fabricante(agente != null ? agente.getFabricante() : null);
            if (faseContraste.getLoteAgente() != null) {
                builder.numeroLote(faseContraste.getLoteAgente().getNumeroLote())
                        .loteFechaCaducidad(faseContraste.getLoteAgente().getFechaCaducidad());
            }
        }
        return builder.build();
    }

    private InyeccionDetalleCompletoResponse.MetadatosInyeccionDTO aMetadatos(Inyeccion i) {
        return InyeccionDetalleCompletoResponse.MetadatosInyeccionDTO.builder()
                .fechaHoraInicio(i.getFechaHoraInicio())
                .fechaHoraFin(i.getFechaHoraFin())
                .duracionSeg(i.getDuracionSeg())
                .sede(i.getInyector().getSala().getSede().getNombre())
                .sala(i.getInyector().getSala().getNombre())
                .inyector(i.getInyector().getNumeroSerie())
                .operador(i.getOperador() != null ? i.getOperador().getNombreCompleto() : null)
                .protocolo(i.getProtocolo() != null ? i.getProtocolo().getNombre() : null)
                .identificadorAnatomico(i.getIdentificadorAnatomico() != null
                        ? i.getIdentificadorAnatomico().getNombre() : null)
                .estado(i.getEstado().name())
                .numeroAccesion(i.getNumeroAccesion())
                .procedimientoProgramado(i.getProcedimientoProgramado())
                .calibreAguja(i.getCalibreAguja())
                .accesoAguja(i.getAccesoAguja())
                .avanceSalinaMl(i.getAvanceSalinaMl())
                .salinaJumpUsado(i.getSalinaJumpUsado())
                .scanner(i.getScanner())
                .notas(i.getNotas())
                .retrasoEscaneoSeg(i.getRetrasoEscaneoSeg())
                .presionMaximaPsi(i.getPresionMaximaPsi())
                .presionPromedioPsi(i.getPresionPromedioPsi())
                .presionLimitePsi(i.getPresionLimitePsi())
                .edaHabilitado(i.getEdaHabilitado())
                .ctdiVolMgy(i.getCtdiVolMgy())
                .dlpMgyCm(i.getDlpMgyCm())
                .build();
    }

    // Arma el comparativo Planeado (protocolo_fases) / Programado
    // (inyeccion_fases_programadas) / Real (inyeccion_fases), unido por
    // numero/orden de fase. Cualquier fase que solo exista en una de las
    // tres fuentes igual aparece en la fila, con los otros dos lados nulos.
    private List<InyeccionDetalleCompletoResponse.ComparativoFaseDTO> aComparativoFases(Inyeccion i) {
        Map<Integer, InyeccionDetalleCompletoResponse.ComparativoFaseDTO.ComparativoFaseDTOBuilder> porFase =
                new LinkedHashMap<>();

        if (i.getProtocolo() != null) {
            List<ProtocoloFase> planeadas = protocoloFaseRepository
                    .findByProtocolo_IdOrderByNumeroFaseAsc(i.getProtocolo().getId());
            for (ProtocoloFase pf : planeadas) {
                int numero = pf.getNumeroFase();
                porFase.computeIfAbsent(numero, n -> InyeccionDetalleCompletoResponse.ComparativoFaseDTO.builder()
                                .numeroFase(n))
                        .planeadoAgente(pf.getAgente() != null ? pf.getAgente().getNombreComercial() : null)
                        .planeadoVolumenMl(pf.getVolumenMl())
                        .planeadoVelocidadFlujoMlS(pf.getVelocidadFlujoMlS());
            }
        }

        List<InyeccionFaseProgramada> programadas = faseProgramadaRepository
                .findByInyeccion_IdOrderByOrdenFaseAsc(i.getId());
        for (InyeccionFaseProgramada fp : programadas) {
            int numero = fp.getOrdenFase();
            porFase.computeIfAbsent(numero, n -> InyeccionDetalleCompletoResponse.ComparativoFaseDTO.builder()
                            .numeroFase(n))
                    .programadoAgente(fp.getAgente() != null ? fp.getAgente().getNombreComercial() : null)
                    .programadoVolumenMl(fp.getVolumenMl())
                    .programadoVelocidadFlujoMlS(fp.getVelocidadFlujoMlS());
        }

        for (InyeccionFase f : i.getFases()) {
            int numero = f.getNumeroFase();
            porFase.computeIfAbsent(numero, n -> InyeccionDetalleCompletoResponse.ComparativoFaseDTO.builder()
                            .numeroFase(n))
                    .realAgente(f.getAgente() != null ? f.getAgente().getNombreComercial() : null)
                    .realVolumenProgramadoMl(f.getVolumenProgramadoMl())
                    .realVolumenRealMl(f.getVolumenRealMl())
                    .realVelocidadFlujoMlS(f.getVelocidadFlujoMlS());
        }

        List<InyeccionDetalleCompletoResponse.ComparativoFaseDTO> resultado = new ArrayList<>();
        porFase.keySet().stream().sorted().forEach(numero ->
                resultado.add(porFase.get(numero).build()));
        return resultado;
    }

    private InyeccionResumenDTO aResumen(Inyeccion i) {
        boolean tieneAlerta = i.getEventosExtravasacion().stream()
                .anyMatch(e -> e.getEstadoEda() == EstadoEda.FUERA_DE_RANGO);

        String agentePrincipal = i.getFases().stream()
                .filter(f -> Boolean.TRUE.equals(f.getEsFaseContraste()))
                .findFirst()
                .map(f -> f.getAgente().getNombreComercial())
                .orElse("—");

        return InyeccionResumenDTO.builder()
                .id(i.getId())
                .fechaHoraInicio(i.getFechaHoraInicio())
                .sala(i.getInyector().getSala().getNombre())
                .inyector(i.getInyector().getNumeroSerie())
                .protocolo(i.getProtocolo() != null ? i.getProtocolo().getNombre() : null)
                .identificadorAnatomico(i.getIdentificadorAnatomico() != null
                        ? i.getIdentificadorAnatomico().getNombre() : null)
                .agentePrincipal(agentePrincipal)
                .volumenCargadoMl(i.getVolumenCargadoMl())
                .volumenTotalMl(i.getVolumenTotalMl())
                .volumenResidualMl(i.getVolumenResidualMl())
                .presionMaximaPsi(i.getPresionMaximaPsi())
                .presionPromedioPsi(i.getPresionPromedioPsi())
                .presionLimitePsi(i.getPresionLimitePsi())
                .edaHabilitado(i.getEdaHabilitado())
                .ctdiVolMgy(i.getCtdiVolMgy())
                .dlpMgyCm(i.getDlpMgyCm())
                .estado(i.getEstado().name())
                .tieneAlertaEda(tieneAlerta)
                .tieneSeriePresion(!i.getSeriePresion().isEmpty())
                .build();
    }
}
