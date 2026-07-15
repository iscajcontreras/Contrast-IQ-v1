package com.contrastiq.backend.service;

import com.contrastiq.backend.dto.EventoExtravasacionDTO;
import com.contrastiq.backend.dto.RevisarExtravasacionRequest;
import com.contrastiq.backend.model.EventoExtravasacion;
import com.contrastiq.backend.model.Usuario;
import com.contrastiq.backend.model.enums.EstadoEda;
import com.contrastiq.backend.repository.EventoExtravasacionRepository;
import com.contrastiq.backend.repository.UsuarioRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExtravasacionService {

    private final EventoExtravasacionRepository repository;
    private final UsuarioRepository usuarioRepository;

    // Usado por GET /api/extravasaciones : soporta el filtro "Solo alertas
    // EDA" del dashboard, mas fecha y estado de revision.
    @Transactional(readOnly = true)
    public Page<EventoExtravasacionDTO> buscar(LocalDateTime desde, LocalDateTime hasta,
                                                String estadoEda, Boolean revisado,
                                                Pageable pageable) {
        Page<EventoExtravasacion> pagina = repository.findAll((root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (desde != null) predicados.add(cb.greaterThanOrEqualTo(root.get("fechaHora"), desde));
            if (hasta != null) predicados.add(cb.lessThanOrEqualTo(root.get("fechaHora"), hasta));
            if (estadoEda != null) predicados.add(cb.equal(root.get("estadoEda"), EstadoEda.valueOf(estadoEda)));
            if (revisado != null) predicados.add(cb.equal(root.get("revisado"), revisado));
            return cb.and(predicados.toArray(new Predicate[0]));
        }, pageable);

        return pagina.map(this::aDto);
    }

    // Cierra el ciclo de la alerta: el radiologo revisa el evento EDA y
    // deja constancia de la accion tomada.
    @Transactional
    public EventoExtravasacionDTO revisar(Long id, RevisarExtravasacionRequest request, Authentication authentication) {
        EventoExtravasacion evento = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El evento no existe"));

        String email = ((Jwt) authentication.getPrincipal()).getSubject();
        Usuario revisor = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado"));

        evento.setRevisado(true);
        evento.setRevisadoPor(revisor);
        evento.setFechaRevision(LocalDateTime.now());
        evento.setAccionTomada(request.getAccionTomada());
        evento.setNotas(request.getNotas());

        return aDto(repository.save(evento));
    }

    private EventoExtravasacionDTO aDto(EventoExtravasacion e) {
        return EventoExtravasacionDTO.builder()
                .id(e.getId())
                .inyeccionId(e.getInyeccion().getId())
                .fechaHora(e.getFechaHora())
                .estadoEda(e.getEstadoEda().name())
                .revisado(e.getRevisado())
                .sala(e.getInyeccion().getInyector().getSala().getNombre())
                .inyector(e.getInyeccion().getInyector().getNumeroSerie())
                .accionTomada(e.getAccionTomada())
                .build();
    }
}
