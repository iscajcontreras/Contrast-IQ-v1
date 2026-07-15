package com.contrastiq.backend.controller;

import com.contrastiq.backend.dto.HistorialInyeccionPacienteDTO;
import com.contrastiq.backend.dto.PacienteDetalleDTO;
import com.contrastiq.backend.dto.PacienteResumenDTO;
import com.contrastiq.backend.dto.ReaccionPacienteDTO;
import com.contrastiq.backend.service.PacienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Dashboard de Paciente: busqueda + perfil + historial de inyecciones.
@RestController
@RequestMapping("/api/pacientes")
@RequiredArgsConstructor
public class PacienteController {

    private final PacienteService pacienteService;

    @GetMapping
    public Page<PacienteResumenDTO> buscar(
            @RequestParam(required = false) String busqueda,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "nombreCompleto"));
        return pacienteService.buscar(busqueda, pageable);
    }

    @GetMapping("/{id}")
    public PacienteDetalleDTO obtenerDetalle(@PathVariable Long id) {
        return pacienteService.obtenerDetalle(id);
    }

    @GetMapping("/{id}/inyecciones")
    public List<HistorialInyeccionPacienteDTO> historialInyecciones(@PathVariable Long id) {
        return pacienteService.historialInyecciones(id);
    }

    @GetMapping("/{id}/reacciones")
    public List<ReaccionPacienteDTO> reacciones(@PathVariable Long id) {
        return pacienteService.reacciones(id);
    }
}
