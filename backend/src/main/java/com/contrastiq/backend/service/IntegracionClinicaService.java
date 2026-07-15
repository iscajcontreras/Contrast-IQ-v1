package com.contrastiq.backend.service;

import com.contrastiq.backend.dto.DatosPacienteHisDTO;
import com.contrastiq.backend.dto.LoteSincronizacionDTO;
import com.contrastiq.backend.model.LoteSincronizacion;
import com.contrastiq.backend.model.Paciente;
import com.contrastiq.backend.repository.LoteSincronizacionRepository;
import com.contrastiq.backend.repository.PacienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class IntegracionClinicaService {

    private final HisIntegracionService hisIntegracionService;
    private final PacienteRepository pacienteRepository;
    private final LoteSincronizacionRepository loteRepository;

    @Transactional
    public DatosPacienteHisDTO sincronizarPacienteDesdeHis(String identificadorExterno) {
        DatosPacienteHisDTO datos = hisIntegracionService.buscarPaciente(identificadorExterno);
        if (datos == null) return null;

        Paciente paciente = pacienteRepository.findByIdentificadorExterno(identificadorExterno)
                .orElseGet(() -> Paciente.builder().identificadorExterno(identificadorExterno).build());

        if (datos.getNombreCompleto() != null) paciente.setNombreCompleto(datos.getNombreCompleto());
        if (datos.getSexo() != null) paciente.setSexo(Paciente.Sexo.valueOf(datos.getSexo()));
        if (datos.getPesoKg() != null) paciente.setPesoKg(datos.getPesoKg());
        if (datos.getAlergias() != null) paciente.setAlergias(datos.getAlergias());
        paciente.setSincronizadoHisEn(LocalDateTime.now());

        pacienteRepository.save(paciente);
        return datos;
    }

    @Transactional(readOnly = true)
    public Page<LoteSincronizacionDTO> historialSincronizacion(Pageable pageable) {
        return loteRepository.findAllByOrderByFechaHoraDesc(pageable).map(this::aDto);
    }

    public LoteSincronizacionDTO aDto(LoteSincronizacion l) {
        return LoteSincronizacionDTO.builder()
                .id(l.getId())
                .fuente(l.getFuente().name())
                .fechaHora(l.getFechaHora())
                .registrosImportados(l.getRegistrosImportados())
                .estado(l.getEstado().name())
                .usuario(l.getUsuario() != null ? l.getUsuario().getNombreCompleto() : "Sistema (automatico)")
                .detalle(l.getDetalle())
                .build();
    }
}
