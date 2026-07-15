package com.contrastiq.backend.controller;

import com.contrastiq.backend.dto.CalibracionProgramadaDTO;
import com.contrastiq.backend.dto.PrediccionFallaDTO;
import com.contrastiq.backend.service.MantenimientoPredictivoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mantenimiento-predictivo")
@RequiredArgsConstructor
public class MantenimientoPredictivoController {

    private final MantenimientoPredictivoService service;

    @GetMapping("/predicciones")
    public List<PrediccionFallaDTO> predicciones() {
        return service.predicciones();
    }

    @GetMapping("/calendario-calibracion")
    public List<CalibracionProgramadaDTO> calendarioCalibracion() {
        return service.calendarioCalibracion();
    }
}
