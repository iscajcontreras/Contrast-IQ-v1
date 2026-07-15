package com.contrastiq.backend.controller;

import com.contrastiq.backend.dto.ChecklistPreInyeccionDTO;
import com.contrastiq.backend.dto.CrearChecklistRequest;
import com.contrastiq.backend.service.ChecklistPreInyeccionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/checklists")
@RequiredArgsConstructor
public class ChecklistPreInyeccionController {

    private final ChecklistPreInyeccionService checklistService;

    @GetMapping("/paciente/{pacienteId}")
    public List<ChecklistPreInyeccionDTO> historialPorPaciente(@PathVariable Long pacienteId) {
        return checklistService.historialPorPaciente(pacienteId);
    }

    @PostMapping
    public ResponseEntity<ChecklistPreInyeccionDTO> crear(
            @Valid @RequestBody CrearChecklistRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(checklistService.crear(request, authentication));
    }
}
