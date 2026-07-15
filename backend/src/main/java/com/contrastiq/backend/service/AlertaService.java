package com.contrastiq.backend.service;

import com.contrastiq.backend.dto.AlertaDTO;
import com.contrastiq.backend.dto.CrearAlertaRequest;
import com.contrastiq.backend.model.AlertaSistema;
import com.contrastiq.backend.model.Inyector;
import com.contrastiq.backend.model.enums.Severidad;
import com.contrastiq.backend.model.enums.TipoAlerta;
import com.contrastiq.backend.repository.AlertaSistemaRepository;
import com.contrastiq.backend.repository.InyectorRepository;
import com.contrastiq.backend.repository.spec.AlertaSistemaSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// Alertas en tiempo real: ademas de guardarlas (para que aparezcan al
// entrar al dashboard), cada alerta nueva se publica de inmediato en
// /topic/alertas via WebSocket, para que quien ya tenga el frontend
// abierto la vea sin recargar ni tener que revisar manualmente.
@Service
@RequiredArgsConstructor
public class AlertaService {

    private final AlertaSistemaRepository alertaRepository;
    private final InyectorRepository inyectorRepository;
    private final SimpMessagingTemplate mensajeria;

    @Transactional(readOnly = true)
    public Page<AlertaDTO> buscar(Boolean resuelta, String severidad, String tipo, Pageable pageable) {
        return alertaRepository.findAll(AlertaSistemaSpecification.conFiltros(resuelta, severidad, tipo), pageable)
                .map(this::aDto);
    }

    @Transactional
    public AlertaDTO crear(CrearAlertaRequest request) {
        Inyector inyector = request.getInyectorId() != null
                ? inyectorRepository.findById(request.getInyectorId())
                        .orElseThrow(() -> new IllegalArgumentException("El inyector indicado no existe"))
                : null;

        AlertaSistema alerta = AlertaSistema.builder()
                .tipo(TipoAlerta.valueOf(request.getTipo()))
                .severidad(Severidad.valueOf(request.getSeveridad()))
                .inyector(inyector)
                .mensaje(request.getMensaje())
                .fechaHora(LocalDateTime.now())
                .resuelta(false)
                .build();

        AlertaDTO dto = aDto(alertaRepository.save(alerta));

        // Push en tiempo real a todos los clientes suscritos
        mensajeria.convertAndSend("/topic/alertas", dto);

        return dto;
    }

    @Transactional
    public AlertaDTO resolver(Long id) {
        AlertaSistema alerta = alertaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("La alerta no existe"));
        alerta.setResuelta(true);
        alerta.setFechaResolucion(LocalDateTime.now());
        return aDto(alertaRepository.save(alerta));
    }

    private AlertaDTO aDto(AlertaSistema a) {
        return AlertaDTO.builder()
                .id(a.getId())
                .tipo(a.getTipo().name())
                .severidad(a.getSeveridad().name())
                .inyector(a.getInyector() != null ? a.getInyector().getNumeroSerie() : null)
                .sala(a.getInyector() != null ? a.getInyector().getSala().getNombre() : null)
                .mensaje(a.getMensaje())
                .fechaHora(a.getFechaHora())
                .resuelta(a.getResuelta())
                .build();
    }
}
