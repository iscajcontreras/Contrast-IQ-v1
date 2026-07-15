package com.contrastiq.backend.service;

import com.contrastiq.backend.dto.ActualizarTicketRequest;
import com.contrastiq.backend.dto.CrearTicketRequest;
import com.contrastiq.backend.dto.TicketSoporteDTO;
import com.contrastiq.backend.model.Inyector;
import com.contrastiq.backend.model.TicketSoporte;
import com.contrastiq.backend.model.Usuario;
import com.contrastiq.backend.model.enums.EstadoTicket;
import com.contrastiq.backend.model.enums.PrioridadTicket;
import com.contrastiq.backend.repository.InyectorRepository;
import com.contrastiq.backend.repository.TicketSoporteRepository;
import com.contrastiq.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// "Tickets de soporte con el fabricante": seguimiento de incidencias
// tecnicas de un inyector que requieren intervencion de Bracco/ACIST.
@Service
@RequiredArgsConstructor
public class TicketSoporteService {

    private final TicketSoporteRepository ticketRepository;
    private final InyectorRepository inyectorRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public List<TicketSoporteDTO> listar() {
        return ticketRepository.findAll().stream()
                .sorted((a, b) -> b.getFechaCreacion().compareTo(a.getFechaCreacion()))
                .map(this::aDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TicketSoporteDTO> listarPorInyector(Long inyectorId) {
        return ticketRepository.findByInyector_IdOrderByFechaCreacionDesc(inyectorId).stream()
                .map(this::aDto)
                .toList();
    }

    @Transactional
    public TicketSoporteDTO crear(CrearTicketRequest request, Authentication authentication) {
        Inyector inyector = inyectorRepository.findById(request.getInyectorId())
                .orElseThrow(() -> new IllegalArgumentException("El inyector no existe"));

        String email = ((Jwt) authentication.getPrincipal()).getSubject();
        Usuario creador = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado"));

        TicketSoporte ticket = TicketSoporte.builder()
                .inyector(inyector)
                .creadoPor(creador)
                .titulo(request.getTitulo())
                .descripcion(request.getDescripcion())
                .prioridad(PrioridadTicket.valueOf(request.getPrioridad()))
                .estado(EstadoTicket.ABIERTO)
                .build();

        return aDto(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketSoporteDTO actualizar(Long id, ActualizarTicketRequest request) {
        TicketSoporte ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El ticket no existe"));

        EstadoTicket nuevoEstado = EstadoTicket.valueOf(request.getEstado());
        ticket.setEstado(nuevoEstado);
        if (request.getNumeroTicketFabricante() != null) {
            ticket.setNumeroTicketFabricante(request.getNumeroTicketFabricante());
        }
        if (request.getRespuestaFabricante() != null) {
            ticket.setRespuestaFabricante(request.getRespuestaFabricante());
        }
        if (nuevoEstado == EstadoTicket.CERRADO) {
            ticket.setFechaCierre(LocalDateTime.now());
        }

        return aDto(ticketRepository.save(ticket));
    }

    private TicketSoporteDTO aDto(TicketSoporte t) {
        return TicketSoporteDTO.builder()
                .id(t.getId())
                .inyectorId(t.getInyector().getId())
                .inyectorNumeroSerie(t.getInyector().getNumeroSerie())
                .sala(t.getInyector().getSala().getNombre())
                .creadoPor(t.getCreadoPor().getNombreCompleto())
                .titulo(t.getTitulo())
                .descripcion(t.getDescripcion())
                .prioridad(t.getPrioridad().name())
                .estado(t.getEstado().name())
                .numeroTicketFabricante(t.getNumeroTicketFabricante())
                .respuestaFabricante(t.getRespuestaFabricante())
                .fechaCreacion(t.getFechaCreacion())
                .fechaCierre(t.getFechaCierre())
                .build();
    }
}
